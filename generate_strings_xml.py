#!/usr/bin/env python3
"""
generate_strings_xml.py
Utility script to synchronize and generate all localized strings.xml files
based on canonical app/src/main/res/values/strings.xml.
"""
import update_translations

if __name__ == "__main__":
    update_translations.update_all_locales()
