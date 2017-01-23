/**
 * © 2012 Simple Finance Technology Corp. All rights reserved.
 * Author: Jarred Ward <jarred@simple.com>
 */

package com.simple.jdub

import com.simple.jdub.Database.Primary
import com.simple.jdub.Database.Role

import java.util.Stack

trait TransactionProvider[R <: Role] {
  def transactionExists: Boolean
  def currentTransaction: Transaction[R]
  def begin(transaction: Transaction[R])(implicit ev: R =:= Primary): Unit
  def end()(implicit ev: R =:= Primary): Unit
  def rollback()(implicit ev: R =:= Primary): Unit
}

class TransactionManager[R <: Role] extends TransactionProvider[R] {
  case class TransactionState(transactions: Stack[Transaction[R]])

  private val localTransactionStorage = new ThreadLocal[Option[TransactionState]] {
    override def initialValue = None
  }

  protected def ambientTransactionState: Option[TransactionState] = {
    localTransactionStorage.get
  }

  protected def ambientTransaction: Option[Transaction[R]] = {
    ambientTransactionState.map(_.transactions.peek)
  }

  protected def currentTransactionState: TransactionState = {
    ambientTransactionState.getOrElse(
      throw new Exception("No transaction in current context")
    )
  }

  def transactionExists: Boolean = {
    ambientTransactionState.isDefined
  }

  def currentTransaction: Transaction[R] = {
    ambientTransaction.getOrElse(
      throw new Exception("No transaction in current context")
    )
  }

  def begin(transaction: Transaction[R])(implicit ev: R =:= Primary): Unit = {
    if (!transactionExists) {
      val stack = new Stack[Transaction[R]]()
      stack.push(transaction)
      localTransactionStorage.set(Some(new TransactionState(stack)))
    } else {
      currentTransactionState.transactions.push(transaction)
    }
  }

  def end()(implicit ev: R =:= Primary): Unit = {
    if (!transactionExists) {
      throw new Exception("No transaction in current context")
    } else {
      currentTransactionState.transactions.pop
      if (currentTransactionState.transactions.empty) {
        localTransactionStorage.set(None)
      }
    }
  }

  def rollback()(implicit ev: R =:= Primary): Unit = {
    currentTransaction.rollback
  }
}
