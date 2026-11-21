//! ThinksMovie core — inti Rust aplikasi Android (dev: Mahiro).
//!
//! UI Kotlin/Compose memanggil fungsi `native*` di bawah lewat JNI; semua
//! jaringan, parsing katalog, dan proxy video dikerjakan di Rust.

pub mod api;
pub mod catalog;
pub mod http;
pub mod proxy;
pub mod util;

use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use serde_json::{json, Value};
use std::panic::{catch_unwind, AssertUnwindSafe};

fn arg(env: &mut JNIEnv, s: &JString) -> String {
    env.get_string(s).map(String::from).unwrap_or_default()
}

/// Bungkus hasil jadi string JSON; error/panic menjadi `{"error": "..."}`.
fn respond(env: &mut JNIEnv, result: std::thread::Result<Result<Value, String>>) -> jstring {
    let text = match result {
        Ok(Ok(v)) => v.to_string(),
        Ok(Err(e)) => json!({ "error": e }).to_string(),
        Err(_) => json!({ "error": "kesalahan internal (panic)" }).to_string(),
    };
    env.new_string(text)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

/// Mulai proxy lokal; mengembalikan port, atau -1 bila gagal.
#[no_mangle]
pub extern "system" fn Java_com_mahiro_thinksmovie_NativeBridge_nativeStartProxy(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    catch_unwind(|| proxy::start().map(|p| p as jint).unwrap_or(-1)).unwrap_or(-1)
}

#[no_mangle]
pub extern "system" fn Java_com_mahiro_thinksmovie_NativeBridge_nativeCatalog(
    mut env: JNIEnv,
    _class: JClass,
    keyword: JString,
    limit: jint,
) -> jstring {
    let keyword = arg(&mut env, &keyword);
    let r = catch_unwind(AssertUnwindSafe(|| api::catalog(&keyword, limit.max(1) as usize)));
    respond(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_com_mahiro_thinksmovie_NativeBridge_nativeSeries(
    mut env: JNIEnv,
    _class: JClass,
    slug: JString,
) -> jstring {
    let slug = arg(&mut env, &slug);
    let r = catch_unwind(AssertUnwindSafe(|| api::series(&slug)));
    respond(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_com_mahiro_thinksmovie_NativeBridge_nativeEpisodeUrl(
    mut env: JNIEnv,
    _class: JClass,
    slug: JString,
    episode: jint,
) -> jstring {
    let slug = arg(&mut env, &slug);
    let r = catch_unwind(AssertUnwindSafe(|| api::episode(&slug, episode as i64)));
    respond(&mut env, r)
}
