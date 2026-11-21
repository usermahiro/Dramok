//! Utilitas kecil: percent-encoding tanpa dependensi tambahan.

/// Percent-encode (setara `urllib.parse.quote(s, safe="")`).
pub fn encode(s: &str) -> String {
    let mut out = String::with_capacity(s.len() * 3);
    for b in s.bytes() {
        match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => out.push(b as char),
            _ => out.push_str(&format!("%{:02X}", b)),
        }
    }
    out
}

/// Percent-decode; urutan `%XX` yang tidak valid dibiarkan apa adanya.
pub fn decode(s: &str) -> String {
    let bytes = s.as_bytes();
    let mut out = Vec::with_capacity(bytes.len());
    let mut i = 0;
    while i < bytes.len() {
        if bytes[i] == b'%' && i + 2 < bytes.len() {
            if let (Some(h), Some(l)) = (hex(bytes[i + 1]), hex(bytes[i + 2])) {
                out.push(h * 16 + l);
                i += 3;
                continue;
            }
        }
        out.push(bytes[i]);
        i += 1;
    }
    String::from_utf8_lossy(&out).into_owned()
}

fn hex(b: u8) -> Option<u8> {
    match b {
        b'0'..=b'9' => Some(b - b'0'),
        b'a'..=b'f' => Some(b - b'a' + 10),
        b'A'..=b'F' => Some(b - b'A' + 10),
        _ => None,
    }
}

/// Slug yang valid hanya berisi `[a-z0-9-]`.
pub fn valid_slug(s: &str) -> bool {
    !s.is_empty() && s.bytes().all(|b| matches!(b, b'a'..=b'z' | b'0'..=b'9' | b'-'))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn roundtrip() {
        let u = "https://cdn.example.com/v/a b.mp4?x=1&y=2%";
        assert_eq!(decode(&encode(u)), u);
    }

    #[test]
    fn decode_edge_cases() {
        assert_eq!(decode("%41%42"), "AB");
        assert_eq!(decode("%4"), "%4");
        assert_eq!(decode("%zz"), "%zz");
        assert_eq!(decode("abc%"), "abc%");
    }

    #[test]
    fn slug() {
        assert!(valid_slug("dubbed-love-story-1"));
        assert!(!valid_slug(""));
        assert!(!valid_slug("a/b"));
        assert!(!valid_slug("Abc"));
    }
}
