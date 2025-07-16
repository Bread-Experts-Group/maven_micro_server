package org.bread_experts_group.maven_microserver

import org.bread_experts_group.protocol.http.HTTPProtocolSelector
import org.bread_experts_group.protocol.http.HTTPRequest
import org.bread_experts_group.protocol.http.HTTPResponse
import org.bread_experts_group.static_microserver.checkAuthorization
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.logging.Logger
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories

@OptIn(ExperimentalEncodingApi::class)
fun httpServerPut(
	logger: Logger,
	selector: HTTPProtocolSelector,
	stores: List<Path>,
	request: HTTPRequest,
	putCredentials: Map<String, String>? = null
) {
	if (!putCredentials.isNullOrEmpty()) {
		val failResponse = checkAuthorization(logger, request, putCredentials)
		if (failResponse != null) {
			selector.sendResponse(failResponse)
			return
		}
	}

	if (request.headers["expect"]?.contains("100-continue") == true)
		selector.sendResponse(HTTPResponse(request, 100))

	var writtenFile: Path? = null
	val storePath = '.' + request.path.path
	stores.forEach {
		val requestedPath = it.resolve(storePath).normalize()
		if (!requestedPath.startsWith(it)) {
			selector.sendResponse(HTTPResponse(request, 404))
			return
		}

		requestedPath.createParentDirectories()
		if (writtenFile == null) {
			val file = FileChannel.open(
				requestedPath,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE
			)
			file.transferFrom(request.data, 0, Long.MAX_VALUE)
			writtenFile = requestedPath
			logger.info { "New file [${file.size()}] written for \"$storePath\" at \"$requestedPath\"" }
			file.close()
		} else {
			logger.fine { "\"$storePath\" copied to \"$requestedPath\" from \"$writtenFile\"" }
			writtenFile.copyTo(requestedPath, true)
		}
	}
	selector.sendResponse(HTTPResponse(request, 204))
}