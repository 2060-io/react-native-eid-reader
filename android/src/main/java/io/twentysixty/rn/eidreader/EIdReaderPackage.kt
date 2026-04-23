package io.twentysixty.rn.eidreader

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider


class EIdReaderPackage : BaseReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
    if (name == EIdReaderModule.NAME) EIdReaderModule(reactContext) else null

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider {
    mapOf(
      EIdReaderModule.NAME to ReactModuleInfo(
        EIdReaderModule.NAME,
        EIdReaderModule.NAME,
        /* canOverrideExistingModule = */ false,
        /* needsEagerInit          = */ false,
        /* isCxxModule             = */ false,
        /* isTurboModule           = */ true,
      )
    )
  }
}
