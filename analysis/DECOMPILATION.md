# Decompilation findings

## Summary

- All nine inputs are valid ZIP/JAR containers.
- SniffRTP class files begin with the normal `CAFEBABE` Java class magic.
- The eight Minecraft 26.2 builds use class-file major version 69 (Java 25).
- The Sponge 1.21.11 build uses class-file major version 65 (Java 21).
- The 26.2 builds share byte-identical core classes.
- Sponge contains a separately compiled variant of the same core.
- There are 59 unique SniffRTP bytecode variants and 119 recovered top-level
  Java files when every platform copy is retained.
- Each bundle also shades roughly 770 Adventure classes under `net.kyori`.
  Those third-party classes were intentionally excluded from the recovered
  SniffRTP source trees.

## Obfuscation and integrity code

The main plugin logic is not encrypted. Classes `internal/v7/A`, `B`, and `C`
contain small integer-array string decoders and SHA-256 integrity checks. The
decoded values identify:

- the load banner (`SniffRTP has been loaded`);
- the author credit (`imsoback` and `sussysus1317-ctrl`);
- the author's GitHub URL;
- paths to `A.class`, `B.class`, and `META-INF/.srp-8f3d1a.bin`; and
- warnings requesting that attribution not be removed.

The 64-byte `.srp-8f3d1a.bin` resource is identical in all nine JARs. It is an
integrity marker, not an encrypted source archive. These classes and the marker
have been preserved rather than bypassed.

## Recovery method

Only the product-owned `dev/deepslate/sniffrtp` namespace was extracted and
decompiled with CFR 0.152. Platform descriptors, the default configuration,
mixins configuration, integrity marker, `pack.mcmeta`, and manifests were
copied without text rewriting.

The resulting Java is decompiler output. It is a close representation of the
bytecode, but it should not be represented as the exact original source text.
