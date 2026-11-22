package com.dayflow.app.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream

/** Imports Front/Back columns from the first worksheet of an .xlsx workbook. */
object ExcelVocabularyImporter {
    fun read(context: Context, uri: Uri): List<WordEntry> {
        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && (entry.name == "xl/sharedStrings.xml" || entry.name == "xl/worksheets/sheet1.xml")) {
                        entries[entry.name] = zip.readBytes()
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("Cannot open selected file")
        val sheet = entries["xl/worksheets/sheet1.xml"] ?: error("No first worksheet found")
        val shared = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val rows = parseRows(sheet, shared)
        if (rows.isEmpty()) error("The Excel sheet is empty")
        val header = rows.first()
        fun column(vararg names: String) = header.indexOfFirst { cell -> names.any { cell.trim().equals(it, true) } }
        val front = column("Front", "Word", "English", "الكلمة", "انجليزي", "إنجليزي")
        val back = column("Back", "Meaning", "Arabic", "المعنى", "عربي", "ترجمة")
        if (front < 0 || back < 0) error("Add column headers named Front and Back")
        return rows.drop(1).mapNotNull { row ->
            val word = row.getOrNull(front).orEmpty().trim()
            val meaning = row.getOrNull(back).orEmpty().trim()
            if (word.isBlank() || meaning.isBlank()) null else WordEntry(System.nanoTime() + word.hashCode(), word, meaning, "")
        }
    }

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val parser = Xml.newPullParser().apply { setInput(xml.inputStream(), "UTF-8") }
        val result = mutableListOf<String>()
        var inside = false
        var value = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "si") { inside = true; value = StringBuilder() }
            else if (event == XmlPullParser.START_TAG && parser.name == "t" && inside) value.append(parser.nextText())
            else if (event == XmlPullParser.END_TAG && parser.name == "si") { result.add(value.toString()); inside = false }
            event = parser.next()
        }
        return result
    }

    private fun parseRows(xml: ByteArray, shared: List<String>): List<List<String>> {
        val parser = Xml.newPullParser().apply { setInput(xml.inputStream(), "UTF-8") }
        val result = mutableListOf<List<String>>()
        var row = mutableMapOf<Int, String>()
        var index = 0
        var type = ""
        var value = ""
        var inText = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> row = mutableMapOf()
                    "c" -> {
                        index = columnIndex(parser.getAttributeValue(null, "r").orEmpty())
                        type = parser.getAttributeValue(null, "t").orEmpty()
                        value = ""
                    }
                    "v", "t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inText) value += parser.text
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> inText = false
                    "c" -> row[index] = if (type == "s") shared.getOrNull(value.toIntOrNull() ?: -1).orEmpty() else value
                    "row" -> if (row.isNotEmpty()) result.add((0..(row.keys.maxOrNull() ?: -1)).map { row[it].orEmpty() })
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun columnIndex(reference: String): Int {
        var result = 0
        reference.takeWhile { it.isLetter() }.uppercase().forEach { result = result * 26 + (it - 'A' + 1) }
        return (result - 1).coerceAtLeast(0)
    }
}