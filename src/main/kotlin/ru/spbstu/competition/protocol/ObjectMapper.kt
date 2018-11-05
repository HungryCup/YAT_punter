package ru.spbstu.competition.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

// Специальный объект, который нужен библиотеке Jackson для
// работы с нестандартными типами данных.
// registerKotlinModule() связывает Jackson и Котлин
val objectMapper = ObjectMapper().registerKotlinModule()
