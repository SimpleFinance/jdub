/**
 * © 2012 Simple Finance Technology Corp. All rights reserved.
 * Author: Jarred Ward <jarred@simple.com>
 */

package com.simple.jdub

import java.util.Stack

trait TransactionProvider {
  def transactionExists: Boolean
  def currentTransaction: Transaction
  def begin(transaction: Transaction): Unit
  def end(): Unit
  def rollback(): Unit
}

class TransactionManager extends TransactionProvider {
  case class TransactionState(transactions: Stack[Transaction])

  private val localTransactionStorage = new ThreadLocal[Option[TransactionState]] {
    override def initialValue = None
  }

  protected def ambientTransactionState: Option[TransactionState] = {
    localTransactionStorage.get
  }

  protected def ambientTransaction: Option[Transaction] = {
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

  def currentTransaction: Transaction = {
    ambientTransaction.getOrElse(
      throw new Exception("No transaction in current context")
    )
  }

  def begin(transaction: Transaction): Unit = {
    if (!transactionExists) {
      val stack = new Stack[Transaction]()
      stack.push(transaction)
      localTransactionStorage.set(Some(new TransactionState(stack)))
    } else {
      currentTransactionState.transactions.push(transaction)
    }
  }

  def end(): Unit = {
    if (!transactionExists) {
      throw new Exception("No transaction in current context")
    } else {
      currentTransactionState.transactions.pop
      if (currentTransactionState.transactions.empty) {
        localTransactionStorage.set(None)
      }
    }
  }

  def rollback(): Unit = {
    currentTransaction.rollback
  }
}
