package org.inquest.uploader.bl

import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.config.IGW2Info
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.api.logs.ILogAnalyzer
import org.inquest.uploader.api.logs.ILogLoader
import org.inquest.uploader.api.logs.ILogUploader
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.bl.config.Config
import org.inquest.uploader.bl.gw2.GW2Info
import org.inquest.uploader.bl.logs.GW2EIParser
import org.inquest.uploader.bl.logs.LogAnalyzer
import org.inquest.uploader.bl.logs.LogLoader
import org.inquest.uploader.bl.logs.LogUploader
import org.inquest.uploader.bl.persistence.Persistence
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance

/**
 * Contains all relevant bindings of this module.
 */
val inquestBlModule: DI.Module = DI.Module("Business Logic") {
    bindSingleton<IConfig> { Config() }

    bindSingleton<IGW2EIParser> {
        GW2EIParser(instance(), instance())
    }

    bindProvider<IPersistence> {
        Persistence(instance(), instance())
    }

    bindSingleton<IGW2Info> {
        GW2Info(instance())
    }

    bindProvider<ILogUploader> {
        LogUploader(instance(), instance(), instance())
    }

    bindSingleton<ILogAnalyzer> {
        LogAnalyzer(instance(), instance())
    }

    bindSingleton<ILogLoader> {
        LogLoader(instance(), instance(), instance())
    }
}