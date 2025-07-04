package org.bread_experts_group.maven_microserver

import org.bread_experts_group.command_line.Flag
import org.bread_experts_group.http.HTTPMethod
import org.bread_experts_group.http.html.DirectoryListing
import org.bread_experts_group.static_microserver.ServerHandle
import org.bread_experts_group.static_microserver.getSocket
import org.bread_experts_group.static_microserver.httpServerGetHead
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
	val color = arguments.getRequired<String>("directory_listing_color").let { if (it == "off") null else it }
	DirectoryListing.css = "color:white;background-color:$color"
	val getHead: ServerHandle = { selector, stores, request, sock ->
		httpServerGetHead(
			selector, stores, request,
			arguments.gets<Pair<String, String>>("get_credential")?.toMap(),
			color != null
		)
	}
	val put: ServerHandle = { selector, stores, request, sock ->
		httpServerPut(
			selector, stores, request,
			arguments.gets<Pair<String, String>>("put_credential")?.toMap()
		)
	}
	staticMain(
		arguments, serverSocket,
		mapOf(
			HTTPMethod.GET to getHead,
			HTTPMethod.HEAD to getHead,
			HTTPMethod.PUT to put
		)
	)
}