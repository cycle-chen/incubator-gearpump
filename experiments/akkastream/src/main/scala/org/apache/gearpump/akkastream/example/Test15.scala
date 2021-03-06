/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gearpump.akkastream.example

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import org.apache.gearpump.akkastream.GearpumpMaterializer
import org.apache.gearpump.cluster.main.{ArgumentsParser, CLIOption}
import org.apache.gearpump.util.AkkaApp

import scala.concurrent.Await
import scala.concurrent.duration._


object Test15 extends AkkaApp with ArgumentsParser {
  // scalastyle:off println
  override val options: Array[(String, CLIOption[Any])] = Array(
    "gearpump" -> CLIOption[Boolean]("<boolean>", required = false, defaultValue = Some(false))
  )

  override def main(akkaConf: Config, args: Array[String]): Unit = {
    val config = parse(args)
    implicit val system = ActorSystem("Test15", akkaConf)
    implicit val materializer: ActorMaterializer = config.getBoolean("gearpump") match {
      case true =>
        GearpumpMaterializer()
      case false =>
        ActorMaterializer(
          ActorMaterializerSettings(system).withAutoFusing(false)
        )
    }
    import akka.stream.scaladsl.GraphDSL.Implicits._
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      val A = builder.add(Source.single(0)).out
      val B = builder.add(Broadcast[Int](2))
      val C = builder.add(Merge[Int](2).named("C"))
      val D = builder.add(Flow[Int].map(_ + 1).named("D"))
      val E = builder.add(Balance[Int](2).named("E"))
      val F = builder.add(Merge[Int](2).named("F"))
      val G = builder.add(Sink.foreach(println).named("G")).in

      C <~ F
      A ~> B ~> C ~> F
      B ~> D ~> E ~> F
      E ~> G

      ClosedShape
    }).run()

    Await.result(system.whenTerminated, 60.minutes)
  }
  // scalastyle:on println
}


