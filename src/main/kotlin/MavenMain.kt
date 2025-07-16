package org.bread_experts_group.maven_microserver

import org.bread_experts_group.command_line.Flag
import org.bread_experts_group.logging.ColoredHandler
import org.bread_experts_group.protocol.http.HTTPMethod
import org.bread_experts_group.protocol.http.header.HTTPForwardedHeader
import org.bread_experts_group.static_microserver.ServerHandle
import org.bread_experts_group.static_microserver.getHead
import org.bread_experts_group.static_microserver.getSocket
import org.bread_experts_group.static_microserver.initGET
import org.bread_experts_group.static_microserver.staticFlags
import org.bread_experts_group.static_microserver.staticMain

fun main(args: Array<String>) {
	val (arguments, serverSocket) = getSocket(
		args,
		"maven_microserver",
		"Distribution of software for Bread Experts Group Maven file servers.",
		staticFlags + listOf(
			Flag(
				"put_credential",
				"A credential required to write files or directories in a store.",
				repeatable = true,
				conv = {
					val (user, passphrase) = it.split(',', limit = 2)
					user to passphrase
				}
			)
		)
	)
	val putCredentials = arguments.gets<Pair<String, String>>("put_credential")?.toMap()
	val put: ServerHandle = { selector, stores, request, sock, arguments ->
		val loggerName = StringBuilder("Maven PUT : ")
		loggerName.append(sock.remoteAddress)
		val logger = ColoredHandler.newLogger(loggerName.toString())
		request.headers["forwarded"]?.let {
			val forwardees = HTTPForwardedHeader.parse(it).forwardees
			logger.info("Request forwarded: $forwardees")
		}
		httpServerPut(logger, selector, stores, request, putCredentials)
	}
	initGET(arguments)
	staticMain(
		arguments, serverSocket,
		mapOf(
			HTTPMethod.GET to getHead,
			HTTPMethod.HEAD to getHead,
			HTTPMethod.PUT to put
		)
	)
}