//! Katalog publik melolo.com: sitemap + API internal situs.
//! Port dari `Catalog` pada `melolo_client.py`.

use crate::http::{get_text, CATALOG_BASE};
use crate::util::{encode, valid_slug};
use serde_json::{json, Value};
use std::collections::HashMap;
use std::sync::{Mutex, OnceLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

const SITEMAP_TTL: Duration = Duration::from_secs(30 * 60);
/// Kunci kandidat untuk URL poster. Nama field sebenarnya belum diverifikasi
/// terhadap respons live, jadi dicoba berurutan; UI punya placeholder.
const COVER_KEYS: [&str; 10] = [
    "cover", "cover_url", "thumb_url", "thumb", "poster", "poster_url", "image", "image_url",
    "thumbnail", "series_cover",
];

#[derive(Clone, Debug, Default)]
pub struct Meta {
    pub name: String,
    pub langs: Value,
    pub episode_num: i64,
    pub cover: String,
    pub episodes: Vec<i64>,
}

impl Meta {
    pub fn to_json(&self) -> Value {
        json!({
            "name": self.name,
            "langs": self.langs,
            "episode_num": self.episode_num,
            "cover": self.cover,
            "episodes": self.episodes,
        })
    }
}

fn sitemap_cache() -> &'static Mutex<Option<(Instant, Vec<String>)>> {
    static C: OnceLock<Mutex<Option<(Instant, Vec<String>)>>> = OnceLock::new();
    C.get_or_init(|| Mutex::new(None))
}

fn meta_cache() -> &'static Mutex<HashMap<String, Meta>> {
    static C: OnceLock<Mutex<HashMap<String, Meta>>> = OnceLock::new();
    C.get_or_init(|| Mutex::new(HashMap::new()))
}

/// Ekstrak slug unik (urutan asli) dari XML sitemap.
pub fn parse_sitemap(xml: &str) -> Vec<String> {
    let mut slugs: Vec<String> = Vec::new();
    for part in xml.split("<loc>").skip(1) {
        let Some(end) = part.find("</loc>") else { continue };
        let loc = part[..end].trim().trim_end_matches('/');
        let Some((_, slug)) = loc.rsplit_once("/dramas/") else { continue };
        if valid_slug(slug) && !slugs.iter().any(|s| s == slug) {
            slugs.push(slug.to_string());
        }
    }
    slugs
}

fn all_slugs() -> Result<Vec<String>, String> {
    {
        let guard = sitemap_cache().lock().map_err(|_| "cache rusak".to_string())?;
        if let Some((t, v)) = guard.as_ref() {
            if t.elapsed() < SITEMAP_TTL {
                return Ok(v.clone());
            }
        }
    }
    let xml = get_text(&format!("{CATALOG_BASE}/sitemap/movie.xml"), None)?;
    let slugs = parse_sitemap(&xml);
    if let Ok(mut g) = sitemap_cache().lock() {
        *g = Some((Instant::now(), slugs.clone()));
    }
    Ok(slugs)
}

/// Daftar drama dub Indonesia (slug memuat "dub"), opsional disaring kata kunci.
pub fn dramas(keyword: &str, limit: usize, only_dub: bool) -> Result<(Vec<String>, usize), String> {
    let mut slugs = all_slugs()?;
    if only_dub {
        slugs.retain(|s| s.contains("dub"));
    }
    let k = keyword.trim().to_lowercase();
    if !k.is_empty() {
        slugs.retain(|s| s.contains(&k.replace(' ', "-")) || s.contains(&k));
    }
    let total = slugs.len();
    slugs.truncate(limit);
    Ok((slugs, total))
}

fn as_i64(v: &Value) -> Option<i64> {
    v.as_i64().or_else(|| v.as_str().and_then(|s| s.trim().parse().ok()))
}

fn api_series(slug: &str, episode_id: Option<i64>, language: &str) -> Result<Value, String> {
    if !valid_slug(slug) {
        return Err("slug tidak valid".into());
    }
    let nonce = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis())
        .unwrap_or(0);
    let mut url = format!(
        "{CATALOG_BASE}/api/series/getSeriesBySlug?language={}&series_slug={}&pageType=play&_refresh={}-{}",
        encode(language),
        encode(slug),
        nonce,
        nonce % 9973
    );
    if let Some(e) = episode_id {
        url.push_str(&format!("&episode_id={e}"));
    }
    let body = get_text(&url, Some(&format!("{CATALOG_BASE}/")))?;
    serde_json::from_str(&body).map_err(|e| format!("respons bukan JSON: {e}"))
}

fn first_item(v: &Value) -> Value {
    v.get("data")
        .and_then(|d| d.get(0))
        .cloned()
        .unwrap_or(Value::Null)
}

pub fn meta_from_item(item: &Value) -> Meta {
    let mut episodes: Vec<i64> = item
        .get("episode_list")
        .and_then(|l| l.as_array())
        .map(|l| l.iter().filter_map(|e| e.get("episode_id").and_then(as_i64)).collect())
        .unwrap_or_default();
    episodes.sort_unstable();
    episodes.dedup();

    let cover = COVER_KEYS
        .iter()
        .filter_map(|k| item.get(*k))
        .find_map(|v| match v {
            Value::String(s) if !s.is_empty() => Some(s.clone()),
            Value::Array(a) => a.first().and_then(|x| x.as_str()).map(str::to_string),
            _ => None,
        })
        .unwrap_or_default();

    Meta {
        name: item.get("name").and_then(|v| v.as_str()).unwrap_or("").to_string(),
        langs: item.get("langs").cloned().unwrap_or(Value::Null),
        episode_num: item
            .get("episode_num")
            .and_then(as_i64)
            .unwrap_or(episodes.len() as i64),
        cover,
        episodes,
    }
}

/// Metadata drama (nama, poster, daftar episode); di-cache per slug.
pub fn series_meta(slug: &str) -> Result<Meta, String> {
    if let Some(m) = meta_cache().lock().ok().and_then(|c| c.get(slug).cloned()) {
        return Ok(m);
    }
    let data = api_series(slug, None, "id")?;
    let item = first_item(&data);
    if item.is_null() {
        return Err("drama tidak ditemukan".into());
    }
    let meta = meta_from_item(&item);
    if let Ok(mut c) = meta_cache().lock() {
        c.insert(slug.to_string(), meta.clone());
    }
    Ok(meta)
}

/// URL MP4 bertanda tangan yang baru untuk satu episode (tidak di-cache karena kedaluwarsa).
pub fn episode_url(slug: &str, episode: i64) -> Result<String, String> {
    let data = api_series(slug, Some(episode), "id")?;
    let item = first_item(&data);
    let list = item.get("episode_list").and_then(|l| l.as_array());
    let url = list
        .into_iter()
        .flatten()
        .find(|e| e.get("episode_id").and_then(as_i64) == Some(episode))
        .and_then(|e| e.get("url"))
        .and_then(|u| u.as_str())
        .unwrap_or("");
    if url.is_empty() {
        Err(format!("URL episode {episode} tidak tersedia"))
    } else {
        Ok(url.to_string())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sitemap_parse() {
        let xml = "<urlset><url><loc>https://melolo.com/dramas/cinta-dub-indo/</loc></url>\
<url><loc>https://melolo.com/dramas/cinta-dub-indo</loc></url>\
<url><loc>https://melolo.com/dramas/other-show</loc></url>\
<url><loc>https://melolo.com/about</loc></url>\
<url><loc>https://melolo.com/dramas/Bad_Slug</loc></url></urlset>";
        assert_eq!(parse_sitemap(xml), vec!["cinta-dub-indo", "other-show"]);
    }

    #[test]
    fn meta_parse() {
        let item = json!({
            "name": "Cinta Dub",
            "langs": "id",
            "episode_num": "3",
            "cover": "https://img/x.jpg",
            "episode_list": [
                {"episode_id": "2", "url": "u2"},
                {"episode_id": 1, "url": "u1"},
                {"episode_id": 3}
            ]
        });
        let m = meta_from_item(&item);
        assert_eq!(m.name, "Cinta Dub");
        assert_eq!(m.episodes, vec![1, 2, 3]);
        assert_eq!(m.episode_num, 3);
        assert_eq!(m.cover, "https://img/x.jpg");
    }

    #[test]
    fn meta_empty() {
        let m = meta_from_item(&json!({}));
        assert!(m.episodes.is_empty());
        assert_eq!(m.episode_num, 0);
        assert!(m.cover.is_empty());
    }
}
