package data

import org.bson.codecs.pojo.annotations.BsonId
import java.io.File

data class Album(@BsonId val name: String, val image: File)