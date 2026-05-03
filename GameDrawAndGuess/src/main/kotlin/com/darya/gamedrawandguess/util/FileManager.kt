package com.darya.gamedrawandguess.util

object FileManager {
    private var words = mutableSetOf<String>()

    init {
        readWordsFromFile()
    }
    private fun readWordsFromFile() {
        val path = "/com/darya/gamedrawandguess/words/words.txt"
        val inputStream = this::class.java.getResourceAsStream(path)
        val lines = inputStream?.bufferedReader()?.use { it.readLines() } ?: emptyList()

        if (lines.isEmpty())
            println("ПРЕДУПРЕЖДЕНИЕ: Файл слов не найден или пуст")

        words = lines.toMutableSet()
    }

    fun getNextWord(): String {
        if (words.isEmpty())
            readWordsFromFile()

        val nextWord = words.random()
        words.remove(nextWord)
        return nextWord
    }
}