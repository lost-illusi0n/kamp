package net.lostillusion.kamp.format

import kotlinx.serialization.KSerializer

internal interface BinaryLengthBasedSerializer<Size: BinarySize<Size>, Element> : KSerializer<Element> {
    val Element.collectionSize: Size
    val sizeSerializer: KSerializer<Size>
}