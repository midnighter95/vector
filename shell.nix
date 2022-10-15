{ pkgs ? import <nixpkgs> {}}:

let
  myLLVM = pkgs.llvmPackages_14;
  espresso = pkgs.stdenv.mkDerivation rec {
    pname = "espresso";
    version = "2.4";
    nativeBuildInputs = [ pkgs.cmake ];
    src = pkgs.fetchFromGitHub {
      owner = "chipsalliance";
      repo = "espresso";
      rev = "v${version}";
      sha256 = "sha256-z5By57VbmIt4sgRgvECnLbZklnDDWUA6fyvWVyXUzsI=";
    };
  };
  circt = pkgs.stdenv.mkDerivation rec {
    pname = "circt";
    version = "r4396.ce85204ca";
    nativeBuildInputs = with pkgs; [ cmake ninja python3 git ];
    src = pkgs.fetchFromGitHub {
      owner = "llvm";
      repo = "circt";
      rev = "da364ea448b70aaa710f9e17063ed745db23b463";
      sha256 = "sha256-oS+wh/hJeJcXj5RoxsUOOX8wMsxgMAMdQtPpyENO0JM=";
      fetchSubmodules = true;
    };
    cmakeFlags = [
      "-S/build/source/llvm/llvm"
      "-DLLVM_ENABLE_PROJECTS=mlir"
      "-DBUILD_SHARED_LIBS=OFF"
      "-DLLVM_STATIC_LINK_CXX_STDLIB=ON"
      "-DLLVM_ENABLE_ASSERTIONS=ON"
      "-DLLVM_BUILD_EXAMPLES=OFF"
      "-DLLVM_ENABLE_BINDINGS=OFF"
      "-DLLVM_ENABLE_OCAMLDOC=OFF"
      "-DLLVM_OPTIMIZED_TABLEGEN=ON"
      "-DLLVM_EXTERNAL_PROJECTS=circt"
      "-DLLVM_EXTERNAL_CIRCT_SOURCE_DIR=/build/source"
      "-DLLVM_BUILD_TOOLS=ON"
    ];
    installPhase = ''
      mkdir -p $out/bin
      mv bin/firtool $out/bin/firtool
    '';
  };

  # nix cc-wrapper will add --gcc-toolchain to clang flags. However, when we want to use
  # our custom libc and compilerrt, clang will only search these libs in --gcc-toolchain 
  # folder. To avoid this wierd behavior of clang, we need to remove --gcc-toolchain options
  # from cc-wrapper
  my-cc-wrapper = let cc = myLLVM.clang; in pkgs.runCommand "cc" {} ''
    mkdir -p "$out"
    cp -rT "${cc}" "$out"
    chmod -R +w "$out"
    sed -i 's/--gcc-toolchain=[^[:space:]]*//' "$out/nix-support/cc-cflags"
    sed -i 's|${cc}|${placeholder "out"}|g' "$out"/bin/* "$out"/nix-support/*
  '';

in pkgs.mkShellNoCC {
    name = "vector";
    buildInputs = with pkgs; [
      myLLVM.llvm
      myLLVM.bintools
      my-cc-wrapper

      jdk mill python3
      parallel protobuf ninja verilator antlr4 numactl dtc glibc_multi cmake
      espresso
      circt

      git cacert # make cmake fetchContent happy
      fmt glog
    ];
    shellHook = ''
      # because we removed --gcc-toolchain from cc-wrapper, we need to add gcc lib path back
      export NIX_LDFLAGS_FOR_TARGET="$NIX_LDFLAGS_FOR_TARGET -L${pkgs.gccForLibs.lib}/lib"
    '';
  }
