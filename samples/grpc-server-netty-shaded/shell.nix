with import <nixpkgs> { };

mkShell {
  name = "env";
  buildInputs = [ gcc zlib zlib.static grpcurl ];
}
