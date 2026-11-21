//! Fungsi tingkat-aplikasi yang mengembalikan JSON; dipanggil oleh lapisan JNI.

use crate::catalog::{dramas, episode_url, series_meta};
use crate::http::CATALOG_BASE;
use crate::proxy::proxy_url;
use serde_json::{json, Value};

/// Daftar slug drama Indonesia: `{ "total": n, "slugs": [...] }`.
pub fn catalog(keyword: &str, limit: usize) -> Result<Value, String> {
    let (slugs, total) = dramas(keyword, limit.max(1))?;
    Ok(json!({ "total": total, "slugs": slugs }))
}

fn absolute(url: &str) -> String {
    if let Some(rest) = url.strip_prefix("//") {
        format!("https://{rest}")
    } else if url.starts_with('/') {
        format!("{CATALOG_BASE}{url}")
    } else {
        url.to_string()
    }
}

/// Metadata drama; `cover_proxy` adalah poster yang sudah lewat proxy Referer.
pub fn series(slug: &str) -> Result<Value, String> {
    let meta = series_meta(slug)?;
    let mut v = meta.to_json();
    if !meta.cover.is_empty() {
        v["cover_proxy"] = Value::String(proxy_url(&absolute(&meta.cover))?);
    }
    Ok(v)
}

/// URL video segar untuk satu episode, sudah lewat proxy: `{ "url": "http://127.0.0.1:..." }`.
pub fn episode(slug: &str, episode: i64) -> Result<Value, String> {
    let raw = episode_url(slug, episode)?;
    Ok(json!({ "url": proxy_url(&raw)? }))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn absolute_urls() {
        assert_eq!(absolute("//cdn.x/a.jpg"), "https://cdn.x/a.jpg");
        assert_eq!(absolute("/img/a.jpg"), "https://melolo.com/img/a.jpg");
        assert_eq!(absolute("https://a/b"), "https://a/b");
    }
}
