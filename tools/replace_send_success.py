import re
from pathlib import Path

path = Path(r"A:\MC bingo server\jamiebingo 1.21.11 development\src\main\java\com\jamie\jamiebingo\bingo\BingoCommands.java")
text = path.read_text(encoding="utf-8")

pattern = r"ctx\.getSource\(\)\.sendSuccess\(\s*\(\)\s*->\s*([^\)]+)\s*,\s*(true|false)\s*\)"
repl = r"com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), () -> \1, \2)"
new_text = re.sub(pattern, repl, text)

# Also replace any remaining direct sendSuccess calls on CommandSourceStack
new_text = re.sub(r"ctx\.getSource\(\)\.sendSuccess\(", "com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), ", new_text)

if new_text != text:
    path.write_text(new_text, encoding="utf-8")
