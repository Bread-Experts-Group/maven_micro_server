package bread_experts_group

import bread_experts_group.http.HTTPMethod

fun main(args: Array<String>) {
	val (singleArgs, multipleArgs, serverSocket) = getSocket(
		args,
		standardFlags + listOf(
			Flag<String>("get_credential", repeatable = true),
			Flag<String>("put_credential", repeatable = true),
			Flag<String>("directory_listing_color", default = "off"),
		)
	)
	val getCredentialTable = multipleArgs["get_credential"]?.associate {
		val credential = (it as String).split(',')
		credential[0] to (credential[1] to credential[2].toBoolean())
	}
	val getHead: ServerHandle = { stores, storePath, request, sock ->
		httpServerGetHead(
			stores, storePath, request, sock,
			(singleArgs["directory_listing_color"] as String).let { if (it == "off") null else it },
			getCredentialTable
		)
	}
	val putCredentialTable = multipleArgs["put_credential"]?.associate {
		val credential = (it as String).split(',')
		credential[0] to credential[1]
	}
	val put: ServerHandle = { stores, storePath, request, sock ->
		httpServerPut(
			stores, storePath, request, sock,
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