package org.bread_experts_group.maven

import org.bread_experts_group.Flag
import org.bread_experts_group.http.HTTPMethod
import org.bread_experts_group.http.html.DirectoryListing
import org.bread_experts_group.static.ServerHandle
import org.bread_experts_group.static.getSocket
import org.bread_experts_group.static.httpServerGetHead
import org.bread_experts_group.static.staticMain

fun main(args: Array<String>) {
	val (singleArgs, multipleArgs, serverSocket) = getSocket(
		args,
		"maven_microserver",
		"Distribution of software for Bread Experts Group Maven file servers.",
		Flag<String>(
			"put_credential",
			"A credential required to write files or directories in a store.",
			repeatable = true
		),
		Flag(
			"directory_listing_color",
			"The CSS background color the directory listing view will show. \"off\" disables the view.",
			default = "off"
		),
		Flag<String>(
			"get_credential",
			"A credential required to access files or directory listings in a store.",
			repeatable = true
		),
		Flag<String>(
			"store",
			"A folder which the server uses to search/write files. The first stores are of higher precedence." +
					" Writes will propagate to all stores.",
			repeatable = true,
			required = 1
		)
	)
	val getCredentialTable = multipleArgs["get_credential"]?.associate {
		val credential = (it as String).split(',')
		credential[0] to credential[1]
	}
	val color = (singleArgs["directory_listing_color"] as String).let { if (it == "off") null else it }
	DirectoryListing.css = "color:white;background-color:$color"
	val getHead: ServerHandle = { stores, request, sock ->
		httpServerGetHead(
			stores, request, sock.outputStream,
			getCredentialTable,
			color != null
		)
	}
	val putCredentialTable = multipleArgs["put_credential"]?.associate {
		val credential = (it as String).split(',')
		credential[0] to credential[1]
	}
	val put: ServerHandle = { stores, request, sock ->
		httpServerPut(
			stores, request,
			sock.inputStream,
			sock.outputStream,
			putCredentialTable
		)
	}
	staticMain(
		multipleArgs, serverSocket,
		mapOf(
			HTTPMethod.GET to getHead,
			HTTPMethod.HEAD to getHead,
			HTTPMethod.PUT to put
		)
	)
}