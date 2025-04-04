package bread_experts_group

fun main(args: Array<String>) {
	val (singleArgs, multipleArgs) = readArgs(
		args
	)
	debug("Hello World!")
	info("Hello World!")
	warn("Hello World!")
	error("Hello World!")
}