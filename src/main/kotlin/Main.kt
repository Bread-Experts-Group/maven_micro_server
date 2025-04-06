package bread_experts_group

import bread_experts_group.http.HTTPMethod
import bread_experts_group.http.HTTPRequest
import bread_experts_group.http.HTTPResponse
import bread_experts_group.http.getHTML
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import javax.net.ssl.SSLException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min

@OptIn(ExperimentalEncodingApi::class)
fun main(args: Array<String>) {
	Thread.currentThread().name = "Maven-Main"
	debug("- Argument read")
	val (singleArgs, multipleArgs) = readArgs(
		args,
		Flag<String>("ip", default = "0.0.0.0"),
		Flag<Int>("port", default = 443, conv = ::stringToInt),
		Flag<Int>("verbosity", default = 1, conv = ::stringToInt),
		Flag<String>("store", repeatable = true),
		Flag<String>("credential", repeatable = true),
	)
	toStringVerbosity = (singleArgs["verbosity"] as? Int) ?: toStringVerbosity
	debug("- Socket retrieval")
	val serverSocket = ServerSocket()
	debug("- Socket bind")
	serverSocket.bind(
		InetSocketAddress(
			singleArgs["ip"] as String,
			singleArgs["port"] as Int
		)
	)
	info("- Server loop")
	val credentialTable = multipleArgs.getValue("credential").associate {
		val credential = (it as String).split(',')
		credential[0] to credential[1]
	}
	val mavenStores = multipleArgs.getValue("store").map { File(it as String).absoluteFile.normalize() }
	while (true) {
		val sock = serverSocket.accept()
		Thread.ofVirtual().name("Maven-${sock.localPort}<${sock.inetAddress}").start {
			try {
				while (true) {
					val request = HTTPRequest.read(sock.inputStream)
					val storePath = request.path.substring(1)
					when (request.method) {
						HTTPMethod.GET, HTTPMethod.HEAD -> mavenStores.firstOrNull {
							val requestedPath = it.resolve(storePath).absoluteFile.normalize()
							if (requestedPath.exists()) {
								if (requestedPath.isFile) {
									debug("Found file for \"$storePath\" at \"${requestedPath.canonicalPath}\"")
									HTTPResponse(
										200, request.version,
										emptyMap(), requestedPath.length()
									).write(sock.outputStream)
									if (request.method == HTTPMethod.GET)
										requestedPath.inputStream().transferTo(sock.outputStream)
									true
								} else if (requestedPath.isDirectory) {
									debug("Directory listing for \"${requestedPath.invariantSeparatorsPath}\"")
									val data = getHTML(it, requestedPath, "color:white;background-color:darkblue")
									HTTPResponse(
										200, request.version,
										mapOf("Content-Type" to "text/html"),
										data.length
									).write(sock.outputStream)
									if (request.method == HTTPMethod.GET)
										sock.outputStream.writeString(data)
									true
								} else false
							} else false
						} ?: run {
							warn("No found file for \"$storePath\"")
							HTTPResponse(404, request.version, emptyMap(), "")
								.write(sock.outputStream)
						}

						HTTPMethod.PUT -> {
							if (credentialTable.isNotEmpty()) {
								val authorization = request.headers["Authorization"]
								if (authorization == null) {
									error("No user provided, unauthorized for PUT")
									HTTPResponse(401, request.version, emptyMap(), "")
										.write(sock.outputStream)
									return@start
								}
								val pair = Base64.decode(authorization.substringAfter("Basic "))
									.decodeToString()
									.split(':')
								val password = credentialTable[pair[0]]
								if (password == null || password != pair[1]) {
									error("${pair[0]} unauthorized for PUT, not a user or wrong password")
									HTTPResponse(403, request.version, emptyMap(), "")
										.write(sock.outputStream)
									return@start
								}
								info("${pair[0]} authorized. Hello")
							}

							val size = request.headers["Content-Length"]?.toLongOrNull()
							if (size == null || size < 1) {
								warn("No size specified for PUT.")
								HTTPResponse(400, request.version, emptyMap(), "")
									.write(sock.outputStream)
							} else {
								var writtenFile: File? = null
								mavenStores.forEach {
									val requestedPath = it.resolve(storePath).absoluteFile.normalize()
									requestedPath.deleteRecursively()
									requestedPath.parentFile.mkdirs()
									if (writtenFile == null) {
										info("New file [$size] written for \"$storePath\" at \"${requestedPath.canonicalPath}\"")
										var remainder = size
										while (remainder > 0) {
											val block = min(remainder.toInt(), 65536)
											requestedPath.writeBytes(sock.inputStream.readNBytes(block))
											remainder -= block
										}
										writtenFile = requestedPath
									} else {
										debug(
											"File [$size] for \"$storePath\" copied to " +
													"\"${requestedPath.canonicalPath}\" from \"${writtenFile.canonicalPath}\""
										)
										writtenFile.copyTo(requestedPath)
									}
								}
								HTTPResponse(204, request.version, emptyMap(), "")
									.write(sock.outputStream)
							}
						}
					}
				}
			} catch (e: SSLException) {
				warn("SSL failure encountered; ${e.localizedMessage}")
				sock.close()
			} catch (e: IOException) {
				warn("IO failure encountered; ${e.localizedMessage}")
				sock.close()
			}
		}
	}
}