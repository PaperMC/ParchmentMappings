/*
 * This file is part of Lorenz, licensed under the MIT License (MIT).
 *
 * Copyright (c) Jamie Mansfield <https://www.jamierocks.uk/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.parchmentmc.util.lorenz

import org.cadixdev.bombe.type.*
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.lorenz.io.TextMappingsReader
import org.cadixdev.lorenz.io.enigma.EnigmaConstants
import org.cadixdev.lorenz.model.ClassMapping
import org.cadixdev.lorenz.model.Mapping
import org.cadixdev.lorenz.model.MethodMapping
import java.io.Reader
import java.util.*

/**
 * An implementation of [org.cadixdev.lorenz.io.MappingsReader] for the Enigma format.
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
class EnigmaReader(reader: Reader) : TextMappingsReader(reader, { Processor() }) {

    open class Processor @JvmOverloads constructor(
        mappings: MappingSet? = MappingSet.create()
    ) : TextMappingsReader.Processor(mappings) {

        protected val stack: Deque<Mapping<*, *>> = ArrayDeque()

        override fun accept(rawLine: String) {
            val indentLevel = getIndentLevel(rawLine)

            // If there is a change in the indentation level, we will need to pop the stack
            // as needed
            while (indentLevel < this.stack.size) {
                this.stack.pop()
            }

            val line = EnigmaConstants.removeComments(rawLine).trim()
            if (line.isEmpty()) return

            // Split up the line, for further processing
            val split = SPACE.split(line)
            val len = split.size

            // Establish the type of mapping
            val key = split[0]
            when (key) {
                CLASS_MAPPING_KEY if len == CLASS_MAPPING_ELEMENT_WITHOUT_DEOBF_COUNT -> {
                    val obfName = this.convertClassName(split[1])
                    this.stack.push(this.readClassMapping(obfName))
                }
                CLASS_MAPPING_KEY if len == CLASS_MAPPING_ELEMENT_WITH_DEOBF_COUNT -> {
                    val obfName = this.convertClassName(split[1])
                    val deobfName = this.convertClassName(split[2])
                    this.stack.push(
                        this.readClassMapping(obfName)
                            .setDeobfuscatedName(deobfName)
                    )
                }
                FIELD_MAPPING_KEY if len == FIELD_MAPPING_ELEMENT_COUNT -> {
                    val obfName = split[1]
                    val deobfName = split[2]
                    val type = this.convertFieldType(FieldType.of(split[3])).toString()
                    this.peekClass().getOrCreateFieldMapping(obfName, type).deobfuscatedName = deobfName
                }
                METHOD_MAPPING_KEY if len == METHOD_MAPPING_ELEMENT_WITHOUT_DEOBF_COUNT -> {
                    val obfName = split[1]
                    val descriptor = this.convertDescriptor(MethodDescriptor.of(split[2])).toString()
                    this.stack.push(this.peekClass().getOrCreateMethodMapping(obfName, descriptor))
                }
                METHOD_MAPPING_KEY if len == METHOD_MAPPING_ELEMENT_WITH_DEOBF_COUNT -> {
                    val obfName = split[1]
                    val deobfName = split[2]
                    val descriptor = this.convertDescriptor(MethodDescriptor.of(split[3])).toString()
                    this.stack.push(
                        this.peekClass().getOrCreateMethodMapping(obfName, descriptor)
                            .setDeobfuscatedName(deobfName)
                    )
                }
                PARAM_MAPPING_KEY if len == PARAM_MAPPING_ELEMENT_COUNT -> {
                    val index = split[1].toInt()
                    val deobfName = split[2]
                    this.peekMethod().getOrCreateParameterMapping(index).deobfuscatedName = deobfName
                }
            }
        }

        protected fun peekClass(): ClassMapping<*, *> {
            if (this.stack.peek() !is ClassMapping) throw UnsupportedOperationException("Not a class on the stack!")
            return this.stack.peek() as ClassMapping<*, *>
        }

        protected fun peekMethod(): MethodMapping {
            if (this.stack.peek() !is MethodMapping) throw UnsupportedOperationException("Not a method on the stack!")
            return this.stack.peek() as MethodMapping
        }

        protected open fun readClassMapping(obfName: String): ClassMapping<*, *> {
            return this.mappings.getOrCreateClassMapping(obfName)
        }

        protected open fun convertClassName(descriptor: String): String {
            if (descriptor.startsWith("none/")) {
                return descriptor.substring("none/".length)
            }
            return descriptor
        }

        protected open fun convertType(type: Type): Type {
            if (type is FieldType) {
                return this.convertFieldType(type)
            }
            return type
        }

        protected open fun convertFieldType(type: FieldType): FieldType {
            if (type is ArrayType) {
                return ArrayType(type.dimCount, this.convertFieldType(type.component))
            }
            if (type is ObjectType) {
                return ObjectType(this.convertClassName(type.className))
            }
            return type
        }

        protected open fun convertDescriptor(descriptor: MethodDescriptor): MethodDescriptor {
            return MethodDescriptor(
                descriptor.paramTypes.stream()
                    .map(::convertFieldType)
                    .toList(),
                this.convertType(descriptor.returnType)
            )
        }

        companion object {
            private const val CLASS_MAPPING_KEY = "CLASS"
            private const val FIELD_MAPPING_KEY = "FIELD"
            private const val METHOD_MAPPING_KEY = "METHOD"
            private const val PARAM_MAPPING_KEY = "ARG"

            private const val CLASS_MAPPING_ELEMENT_WITH_DEOBF_COUNT = 3
            private const val CLASS_MAPPING_ELEMENT_WITHOUT_DEOBF_COUNT = 2
            private const val FIELD_MAPPING_ELEMENT_COUNT = 4
            private const val METHOD_MAPPING_ELEMENT_WITH_DEOBF_COUNT = 4
            private const val METHOD_MAPPING_ELEMENT_WITHOUT_DEOBF_COUNT = 3
            private const val PARAM_MAPPING_ELEMENT_COUNT = 3

            private fun getIndentLevel(line: String): Int {
                var indentLevel = 0
                for (i in 0..<line.length) {
                    if (line[i] != '\t') break
                    indentLevel++
                }
                return indentLevel
            }
        }
    }
}
