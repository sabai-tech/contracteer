package tech.sabai.contracteer.cli

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Model.UsageMessageSpec.*

@Command(
  name = "contracteer",
  headerHeading = "\n",
  header = ["Contracteer - the musketeer of your contracts"],
  synopsisHeading = "\n@|bold,cyan Usage|@:\n  ",
  descriptionHeading = "\n@|bold,cyan Description|@:\n  ",
  description = ["A tool for validating API contracts and running mock servers based on OpenAPI 3 Specifications."],
  optionListHeading = "\n@|bold,cyan Options|@:\n",
  commandListHeading = "\n@|bold,cyan Commands|@:\n",
  mixinStandardHelpOptions = true,
  usageHelpAutoWidth = true,
  subcommands = [VerifyCli::class, MockCli::class]
)
class Cli

fun main(args: Array<String>) {
  val sectionKeys = listOf(
    SECTION_KEY_HEADER_HEADING,
    SECTION_KEY_HEADER,
    SECTION_KEY_DESCRIPTION_HEADING,
    SECTION_KEY_DESCRIPTION,
    SECTION_KEY_SYNOPSIS_HEADING,
    SECTION_KEY_SYNOPSIS,
    SECTION_KEY_PARAMETER_LIST_HEADING,
    SECTION_KEY_PARAMETER_LIST,
    SECTION_KEY_OPTION_LIST_HEADING,
    SECTION_KEY_OPTION_LIST,
    SECTION_KEY_COMMAND_LIST_HEADING,
    SECTION_KEY_COMMAND_LIST,
  )
  val cmd = CommandLine(Cli())
  cmd.commandSpec.usageMessage().sectionKeys(sectionKeys)
  cmd.subcommands.values.forEach { it.commandSpec.usageMessage().sectionKeys(sectionKeys) }
  cmd.execute(* args)
}
