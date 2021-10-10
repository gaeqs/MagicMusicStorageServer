package util

import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.progress.EncoderProgressListener
import java.io.File

object ConversionUtils {

    /**
     * Transforms the given [source] file and transforms its multimedia content into a MP3 audio file,
     * writing the result into the [target] file.
     *
     * This process can be listened using a [EncoderProgressListener].
     *
     * @param source the source file.
     * @param target the target file.
     * @param listener the [EncoderProgressListener] you can use to listen the conversion.
     */
    fun convertToMP3(source: File, target: File, listener: EncoderProgressListener?): Boolean {
        try {
            val multimediaObject = MultimediaObject(source)
            val info = multimediaObject.info
            info.audio ?: return false

            val audioAttributes = AudioAttributes()
            audioAttributes.setCodec("libmp3lame")
            audioAttributes.setBitRate(192000)
            audioAttributes.setChannels(2)
            audioAttributes.setSamplingRate(44100)

            val encodingAttributes = EncodingAttributes()
            encodingAttributes.setOutputFormat("mp3")
            encodingAttributes.setAudioAttributes(audioAttributes)

            val encoder = Encoder()
            encoder.encode(multimediaObject, target, encodingAttributes, listener)
            return true
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

}