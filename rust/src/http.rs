//! Lapisan HTTP (ureq + rustls, sertifikat root dibundel sehingga tidak
//! bergantung pada trust store Android).

use std::io::Read;
use std::sync::OnceLock;
use std::time::Duration;

pub const CATALOG_BASE: &str = "https://melolo.com";
pub const USER_AGENT: &str = "Mozilla/5.0 (Linux; Android 14; ThinksMovie) AppleWebKit/537.36 \
(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36 ThinksMovie/0.1";

fn agent() -> &'static ureq::Agent {
    static AGENT: OnceLock<ureq::Agent> = OnceLock::new();
    AGENT.get_or_init(|| {
        ureq::AgentBuilder::new()
            .timeout_connect(Duration::from_secs(15))
            .timeout_read(Duration::from_secs(30))
            .build()
    })
}

/// GET sebagai teks (maks. 64 MB, cukup untuk sitemap besar).
pub fn get_text(url: &str, referer: Option<&str>) -> Result<String, String> {
    let mut req = agent().get(url).set("User-Agent", USER_AGENT);
    if let Some(r) = referer {
        req = req.set("Referer", r);
    }
    match req.call() {
        Ok(resp) => {
            let mut buf = String::new();
            resp.into_reader()
                .take(64 * 1024 * 1024)
                .read_to_string(&mut buf)
                .map_err(|e| e.to_string())?;
            Ok(buf)
        }
        Err(ureq::Error::Status(code, _)) => Err(format!("HTTP {code}")),
        Err(e) => Err(e.to_string()),
    }
}

/// GET mentah untuk proxy; menerima `Range` dan selalu mengirim `Referer`.
pub fn open(url: &str, referer: &str, range: Option<&str>) -> Result<ureq::Response, ureq::Error> {
    let mut req = agent()
        .get(url)
        .set("User-Agent", USER_AGENT)
        .set("Referer", referer);
    if let Some(r) = range {
        req = req.set("Range", r);
    }
    req.call()
}
