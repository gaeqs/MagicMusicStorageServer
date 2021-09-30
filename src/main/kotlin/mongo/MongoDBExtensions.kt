package mongo

import com.mongodb.client.model.Filters
import org.bson.conversions.Bson

infix fun String.eq(other: Any): Bson = Filters.eq(this, other)