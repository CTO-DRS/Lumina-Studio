# ============================================================
# Lumina Studio — R8 / ProGuard configuration
# Organized by concern. Nothing here is required for correctness
# of the engine layer (no reflection-based access anywhere);
# rules below only improve release diagnostics.
# ============================================================

# --- Readable release stack traces ---------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin metadata (required by coroutines/Room tooling) ---
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# --- WebView JS interfaces (none in this app; kept as doc) ---
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
