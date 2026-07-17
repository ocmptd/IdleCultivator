package com.ocmptd.idlecultivator.command;

import java.nio.file.Path;
import java.util.List;

public record CommandReply(String text, List<Path> images) {
}
