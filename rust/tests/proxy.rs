//! Uji integrasi proxy: upstream palsu memverifikasi Referer & Range diteruskan.

use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::{Arc, Mutex};
use std::thread;
use thinksmovie_core::proxy::proxy_url;

fn fake_upstream(seen: Arc<Mutex<String>>) -> u16 {
    let l = TcpListener::bind("127.0.0.1:0").unwrap();
    let port = l.local_addr().unwrap().port();
    thread::spawn(move || {
        for s in l.incoming().flatten() {
            let seen = seen.clone();
            thread::spawn(move || {
                let mut s = s;
                let mut buf = [0u8; 4096];
                let n = s.read(&mut buf).unwrap_or(0);
                *seen.lock().unwrap() = String::from_utf8_lossy(&buf[..n]).to_string();
                let _ = s.write_all(
                    b"HTTP/1.1 206 Partial Content\r\nContent-Type: video/mp4\r\n\
Content-Length: 5\r\nContent-Range: bytes 0-4/100\r\nAccept-Ranges: bytes\r\n\
Connection: close\r\n\r\nhello",
                );
            });
        }
    });
    port
}

fn raw_get(port: u16, path: &str, extra: &str) -> String {
    let mut s = TcpStream::connect(("127.0.0.1", port)).unwrap();
    write!(s, "GET {path} HTTP/1.1\r\nHost: x\r\n{extra}\r\n").unwrap();
    let mut out = String::new();
    s.read_to_string(&mut out).unwrap();
    out
}

#[test]
fn proxy_forwards_referer_and_range() {
    let seen = Arc::new(Mutex::new(String::new()));
    let up = fake_upstream(seen.clone());
    let url = proxy_url(&format!("http://127.0.0.1:{up}/video.mp4")).unwrap();
    let rest = url.strip_prefix("http://127.0.0.1:").unwrap();
    let (port, path) = rest.split_once('/').unwrap();
    let port: u16 = port.parse().unwrap();

    let resp = raw_get(port, &format!("/{path}"), "Range: bytes=0-4\r\n");
    assert!(resp.starts_with("HTTP/1.1 206"), "{resp}");
    assert!(resp.contains("Content-Range: bytes 0-4/100"), "{resp}");
    assert!(resp.ends_with("hello"), "{resp}");

    let up_req = seen.lock().unwrap().to_lowercase();
    assert!(up_req.contains("referer: https://melolo.com/"), "{up_req}");
    assert!(up_req.contains("range: bytes=0-4"), "{up_req}");

    // Tanpa token yang benar harus ditolak.
    let bad = raw_get(port, "/?t=salah&u=http%3A%2F%2F127.0.0.1%2F", "");
    assert!(bad.starts_with("HTTP/1.1 403"), "{bad}");
}
