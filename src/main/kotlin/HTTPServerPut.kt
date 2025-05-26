package org.bread_experts_group.maven

import org.bread_experts_group.http.HTTPRequest
import org.bread_experts_group.http.HTTPResponse
import org.bread_experts_group.logging.ColoredLogger
import org.bread_experts_group.static.checkAuthorization
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min

private val putLogger = ColoredLogger.newLogger("Maven Server PUT")

@OptIn(ExperimentalEncodingApi::class)
fun httpServerPut(
	stores: List<File>,
	request: HTTPRequest,
	sIn: InputStream,
	sOut: OutputStream,
	putCredentials: Map<String, String>? = null
) {
	if (!putCredentials.isNullOrEmpty()) {
		val failResponse = checkAuthorization(request, putCredentials)
		if (failResponse != null) {
			failResponse.write(sOut)
			return
		}
	}

	val size = request.headers["Content-Length"]?.toLongOrNull()
	if (size == null || size < 1) {
		putLogger.warning("No size specified for PUT.")
		HTTPResponse(400, request.version)
			.write(sOut)
		return
	}

	var writtenFile: File? = null
	HTTPResponse(100, request.version)
		.write(sOut)
	val storePath = '.' + request.path.path
	stores.forEach {
		val requestedPath = it.resolve(storePath).absoluteFile.normalize()
		requestedPath.parentFile.mkdirs()
		if (writtenFile == null) {
			val fileStream = FileOutputStream(requestedPath)
			var remainder = size
			while (remainder > 0) {
				val block = min(remainder, 2048)
				fileStream.write(sIn.readNBytes(block.toInt()))
				fileStream.flush()
				remainder -= block
			}
			fileStream.close()
			writtenFile = requestedPath
			putLogger.info { "New file [$size] written for \"$storePath\" at \"${requestedPath.canonicalPath}\"" }
		} else {
			putLogger.fine {
				"File [$size] for \"$storePath\" copied to " +
						"\"${requestedPath.canonicalPath}\" from \"${writtenFile.canonicalPath}\""
			}
			writtenFile.copyTo(requestedPath, true)
		}
	}
	HTTPResponse(204, request.version)
		.write(sOut)
}