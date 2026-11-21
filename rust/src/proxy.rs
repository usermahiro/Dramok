//! Proxy HTTP lokal (127.0.0.1) yang menambahkan header `Referer` agar CDN
//! video/poster mengizinkan playback. Port dari `ProxyHandler` pada
//! `melolo_gui.py`, dengan tambahan token acak agar aplikasi lain di perangkat
//! tidak bisa memakainya sebagai open proxy.

use crate::http::{open, CATALOG_BASE};
use crate::util::{decode, encode};
use std::io::{self, BufRead, BufReader, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::OnceLock;
use std::thread;
use std::time::Duration;

struct Proxy {
    port: u16,
    token: String,
}

static PROXY: OnceLock<Proxy> = OnceLock::new();

fn make_token() -> String {
    use std::collections::hash_map::RandomState;
    use std::hash::{BuildHasher, Hasher};
    let a = RandomState::new().build_hasher().finish();
    let b = RandomState::new().build_hasher().finish();
    format!("{a:016x}{b:016x}")
}

fn bind() -> Proxy {
    let fail = || Proxy { port: 0, token: String::new() };
    let Ok(listener) = TcpListener::bind(("127.0.0.1", 0)) else { return fail() };
    let Ok(addr) = listener.local_addr() else { return fail() };
    let token = make_token();
    let t = token.clone();
    thread::spawn(move || {
        for stream in listener.incoming().flatten() {
            let t = t.clone();
            thread::spawn(move || {
                let _ = handle(stream, &t);
            });
        }
    });
    Proxy { port: addr.port(), token }
}

/// Jalankan proxy (idempoten) dan kembalikan port-nya.
pub fn start() -> Result<u16, String> {
    let p = PROXY.get_or_init(bind);
    if p.port == 0 {
        Err("gagal membuka proxy lokal".into())
    } else {
        Ok(p.port)
    }
}

/// Ubah URL CDN menjadi URL proxy lokal yang bisa langsung diberikan ke pemutar.
pub fn proxy_url(url: &str) -> Result<String, String> {
    start()?;
    let p = PROXY.get().ok_or("proxy belum siap")?;
    Ok(format!("http://127.0.0.1:{}/?t={}&u={}", p.port, p.token, encode(url)))
}

/// Ambil URL tujuan dari request line; `Err(status)` bila ditolak.
pub fn parse_target(request_line: &str, token: &str) -> Result<String, u16> {
    let mut parts = request_line.split_whitespace();
    let (method, path) = (parts.next().unwrap_or(""), parts.next().unwrap_or(""));
    if method != "GET" {
        return Err(405);
    }
    let query = path.split_once('?').map(|(_, q)| q).unwrap_or("");
    let (mut t, mut u) = (None, None);
    for pair in query.split('&') {
        match pair.split_once('=') {
            Some(("t", v)) => t = Some(v),
            Some(("u", v)) => u = Some(decode(v)),
            _ => {}
        }
    }
    if t != Some(token) {
        return Err(403);
    }
    match u {
        Some(url) if url.starts_with("http://") || url.starts_with("https://") => Ok(url),
        _ => Err(400),
    }
}

fn reason(code: u16) -> &'static str {
    match code {
        200 => "OK",
        206 => "Partial Content",
        400 => "Bad Request",
        403 => "Forbidden",
        404 => "Not Found",
        405 => "Method Not Allowed",
        416 => "Range Not Satisfiable",
        _ => "Upstream",
    }
}

fn simple(stream: &mut TcpStream, code: u16) -> io::Result<()> {
    write!(
        stream,
        "HTTP/1.1 {code} {}\r\nContent-Length: 0\r\nConnection: close\r\n\r\n",
        reason(code)
    )
}

fn relay(stream: &mut TcpStream, resp: ureq::Response) -> io::Result<()> {
    let status = resp.status();
    let mut head = format!("HTTP/1.1 {status} {}\r\n", reason(status));
    for h in ["Content-Type", "Content-Length", "Accept-Ranges", "Content-Range"] {
        if let Some(v) = resp.header(h) {
            head.push_str(&format!("{h}: {v}\r\n"));
        }
    }
    head.push_str("Connection: close\r\n\r\n");
    stream.write_all(head.as_bytes())?;
    io::copy(&mut resp.into_reader(), stream)?;
    stream.flush()
}

fn handle(mut stream: TcpStream, token: &str) -> io::Result<()> {
    stream.set_read_timeout(Some(Duration::from_secs(15)))?;
    let mut reader = BufReader::new(stream.try_clone()?);
    let mut request_line = String::new();
    reader.read_line(&mut request_line)?;

    let mut range: Option<String> = None;
    let mut total = 0usize;
    loop {
        let mut line = String::new();
        let n = reader.read_line(&mut line)?;
        total += n;
        if n == 0 || line == "\r\n" || line == "\n" || total > 16 * 1024 {
            break;
        }
        if let Some((k, v)) = line.split_once(':') {
            if k.eq_ignore_ascii_case("range") {
                range = Some(v.trim().to_string());
            }
        }
    }

    let url = match parse_target(&request_line, token) {
        Ok(u) => u,
        Err(code) => return simple(&mut stream, code),
    };
    let referer = format!("{CATALOG_BASE}/");
    match open(&url, &referer, range.as_deref()) {
        Ok(resp) => relay(&mut stream, resp),
        // Status error (404, 416, ...) diteruskan apa adanya ke pemutar.
        Err(ureq::Error::Status(_, resp)) => relay(&mut stream, resp),
        Err(_) => simple(&mut stream, 502),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn target_parsing() {
        let l = "GET /?t=abc&u=https%3A%2F%2Fcdn.x%2Fv.mp4%3Fa%3D1 HTTP/1.1";
        assert_eq!(parse_target(l, "abc"), Ok("https://cdn.x/v.mp4?a=1".to_string()));
        assert_eq!(parse_target(l, "zzz"), Err(403));
        assert_eq!(parse_target("POST / HTTP/1.1", "abc"), Err(405));
        assert_eq!(parse_target("GET /?t=abc&u=file%3A%2F%2F%2Fetc HTTP/1.1", "abc"), Err(400));
        assert_eq!(parse_target("GET /?t=abc HTTP/1.1", "abc"), Err(400));
    }
}
