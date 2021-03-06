/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal

import org.neo4j.cypher.internal.compiled_runtime.v3_2.{BuildCompiledExecutionPlan, CompiledRuntimeContext}
import org.neo4j.cypher.internal.compiler.v3_2._
import org.neo4j.cypher.internal.compiler.v3_2.phases.CompilationState
import org.neo4j.cypher.internal.frontend.v3_2.InvalidArgumentException
import org.neo4j.cypher.internal.frontend.v3_2.notification.RuntimeUnsupportedNotification
import org.neo4j.cypher.internal.frontend.v3_2.phases.{Do, If, Transformer}

object EnterpriseRuntimeBuilder extends RuntimeBuilder[Transformer[CompiledRuntimeContext, CompilationState, CompilationState]] {
  def create(runtimeName: Option[RuntimeName], useErrorsOverWarnings: Boolean): Transformer[CompiledRuntimeContext, CompilationState, CompilationState] = runtimeName match {
    case None =>
      BuildCompiledExecutionPlan andThen
      If[CompiledRuntimeContext, CompilationState, CompilationState](_.maybeExecutionPlan.isEmpty) {
        BuildInterpretedExecutionPlan
      }

    case Some(InterpretedRuntimeName) =>
      BuildInterpretedExecutionPlan

    case Some(CompiledRuntimeName) if useErrorsOverWarnings =>
      BuildCompiledExecutionPlan andThen
      If[CompiledRuntimeContext, CompilationState, CompilationState](_.maybeExecutionPlan.isEmpty)(
        Do(_ => throw new InvalidArgumentException("The given query is not currently supported in the selected runtime"))
      )

    case Some(CompiledRuntimeName) =>
      BuildCompiledExecutionPlan andThen
      If[CompiledRuntimeContext, CompilationState, CompilationState](_.maybeExecutionPlan.isEmpty)(
        Do((_: CompiledRuntimeContext).notificationLogger.log(RuntimeUnsupportedNotification)) andThen
        BuildInterpretedExecutionPlan
      )

    case Some(x) => throw new InvalidArgumentException(s"This version of Neo4j does not support requested runtime: $x")
  }
}
