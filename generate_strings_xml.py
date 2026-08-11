import re
import os

def escape_xml_val(val):
    # Escape special characters for Android strings.xml
    val = val.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    # Escape single quotes and double quotes for Android
    val = val.replace("'", "\\'")
    val = val.replace('"', '\\"')
    return val

with open("app/src/main/java/com/example/ui/Localization.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Locate the translations block
translations_match = re.search(r'private val translations = mapOf\((.*?)\n\s*\)\s*\n\s*fun getString', content, re.DOTALL)
if not translations_match:
    print("Could not find translations map")
    exit(1)

translations_block = translations_match.group(1)

# Extract each language block
# Format: "lang_code" to mapOf(...)
lang_matches = re.finditer(r'"([a-z_]{2,5})"\s+to\s+mapOf\((.*?)\),?\s*(?="\w+"\s+to\s+mapOf\(|\Z)', translations_block, re.DOTALL)

languages = {}
for match in lang_matches:
    lang = match.group(1)
    map_content = match.group(2)
    # Parse keys and values
    pairs = re.findall(r'"([^"]+)"\s+to\s+"([^"]*)"', map_content)
    languages[lang] = {k: v for k, v in pairs}

# Map language codes to Android values directory suffixes
lang_to_folders = {
    "en": ["values"],
    "es": ["values-es"],
    "fr": ["values-fr"],
    "de": ["values-de"],
    "it": ["values-it"],
    "pt": ["values-pt"],
    "ru": ["values-ru"],
    "zh": ["values-zh", "values-zh-rCN"],
    "zh_tw": ["values-zh-rTW"],
    "ja": ["values-ja"],
    "ko": ["values-ko"],
    "vi": ["values-vi"],
    "ar": ["values-ar"],
    "hi": ["values-hi"],
    "uk": ["values-uk"],
    "pl": ["values-pl"],
    "tr": ["values-tr"],
    "id": ["values-in", "values-id"],
    "nl": ["values-nl"],
    "sv": ["values-sv"],
    "th": ["values-th"],
    "ms": ["values-ms"],
    "cs": ["values-cs"],
    "he": ["values-iw", "values-he"],
    "da": ["values-da"],
    "fi": ["values-fi"],
    "no": ["values-no"],
    "fa": ["values-fa"],
    "el": ["values-el"],
    "hu": ["values-hu"],
    "ro": ["values-ro"],
    "bn": ["values-bn"],
    "pa": ["values-pa"],
    "ur": ["values-ur"],
    "sk": ["values-sk"],
    "hr": ["values-hr"],
    "bg": ["values-bg"],
    "lt": ["values-lt"],
    "lv": ["values-lv"],
    "et": ["values-et"],
    "sl": ["values-sl"],
    "sr": ["values-sr"],
    "ca": ["values-ca"],
    "gl": ["values-gl"],
    "eu": ["values-eu"],
    "gu": ["values-gu"],
    "kn": ["values-kn"],
    "ml": ["values-ml"],
    "ta": ["values-ta"],
    "te": ["values-te"],
    "mr": ["values-mr"],
    "az": ["values-az"],
    "ka": ["values-ka"],
    "hy": ["values-hy"],
    "kk": ["values-kk"],
    "uz": ["values-uz"],
    "sw": ["values-sw"],
    "af": ["values-af"],
    "is": ["values-is"],
    "tl": ["values-tl"]
}

for lang, translation_map in languages.items():
    if lang == "en":
        # English is default values/strings.xml, which is already present
        continue
    
    folders = lang_to_folders.get(lang, [f"values-{lang}"])
    
    # Generate the strings.xml content
    xml_content = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
    for key, val in sorted(translation_map.items()):
        escaped_val = escape_xml_val(val)
        xml_content += f'    <string name="{key}">{escaped_val}</string>\n'
    xml_content += '</resources>\n'
    
    for folder in folders:
        dir_path = os.path.join("app/src/main/res", folder)
        os.makedirs(dir_path, exist_ok=True)
        file_path = os.path.join(dir_path, "strings.xml")
        with open(file_path, "w", encoding="utf-8") as out_f:
            out_f.write(xml_content)
        print(f"Generated: {file_path}")

print("All translation files generated successfully!")
