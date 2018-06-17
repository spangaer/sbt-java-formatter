/*
 * Copyright 2016 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.sbt

import com.lightbend.sbt.javaformatter.JavaFormatter
import sbt._
import sbt.Keys._

import scala.annotation.tailrec

object AutomateJavaFormatterPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin

  override def projectSettings = automateFor(Compile, Test)

  def automateFor(configurations: Configuration*): Seq[Setting[_]] = configurations.foldLeft(List.empty[Setting[_]]) {
    _ ++ inConfig(_)(compile := compile.dependsOn(JavaFormatterPlugin.JavaFormatterKeys.format).value)
  }
}

object JavaFormatterPlugin extends AutoPlugin {

  object JavaFormatterKeys {
    val format: TaskKey[Seq[File]] =
      TaskKey("javafmt", "Format Java sources")
  }

  val autoImport = JavaFormatterKeys
  import autoImport._

  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin

  override def projectSettings = settingsFor(Compile, Test) ++ notToBeScopedSettings

  def settingsFor(configurations: Configuration*): Seq[Setting[_]] = configurations.foldLeft(List.empty[Setting[_]]) {
    _ ++ inConfig(_)(toBeScopedSettings)
  }

  def settingsFromProfile(file: File): Map[String, String] = {
    val xml = scala.xml.XML.loadFile(file)
    (xml \\ "setting").foldLeft(Map.empty[String, String]) {
      case (r, node) => r.updated((node \ "@id").text, (node \ "@value").text)
    }
  }

  def toBeScopedSettings: Seq[Setting[_]] =
    List(
      (sourceDirectories in format) := List(javaSource.value),
      format := {
        val streamz = streams.value
        val log = streamz.log
        val sD = (sourceDirectories in format).value.toList
        val iF = (includeFilter in format).value
        val eF = (excludeFilter in format).value
        val tPR = thisProjectRef.value
        val c = configuration.value
        JavaFormatter(
          sD,
          iF,
          eF,
          tPR,
          c,
          streamz)
      }
    )

  def notToBeScopedSettings: Seq[Setting[_]] =
    List(
      includeFilter in format := "*.java"
    )

}
