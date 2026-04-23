require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-eid-reader"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "16.0" }
  s.source       = { :git => "https://github.com/2060-io/react-native-eid-reader.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  # Keep the TurboModule header out of the pod's umbrella. It imports
  # `<RNEIdReaderSpec/RNEIdReaderSpec.h>` which transitively pulls in C++
  # headers (e.g. <utility>, std::optional); if it ends up in the umbrella,
  # other pods compiling plain Obj-C files that parse this module will fail
  # with "This file must be compiled as Obj-C++" / "'utility' file not found".
  s.private_header_files = "ios/EidReader.h"

  s.dependency "OpenSSL-Universal", '3.2.2000'

  # `install_modules_dependencies` (RN >= 0.71) wires React-Core, React-Codegen
  # (when New Arch is enabled), RCT-Folly, and the TurboModule core
  # automatically, and propagates `RCT_NEW_ARCH_ENABLED` into this pod.
  install_modules_dependencies(s)
end
