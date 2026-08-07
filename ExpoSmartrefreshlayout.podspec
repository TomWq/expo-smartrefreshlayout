require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "ExpoSmartrefreshlayout"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]
  s.platforms    = { :ios => "15.1" }
  s.source       = { :git => "https://github.com/TomWq/expo-smartrefreshlayout.git", :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,mm,swift,cpp}"
  s.private_header_files = "ios/**/*.h"
  # SmartRefreshControl's core/Classics/Material sources are vendored under
  # ios/SmartRefreshControl so consumers do not depend on an unmaintained
  # remote pod.

  # Provided by React Native's react_native_pods.rb. This wires the Fabric and
  # Codegen dependencies without using Expo Modules API.
  install_modules_dependencies(s)
end
