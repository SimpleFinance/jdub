/**
 * © 2012 Simple Finance Technology Corp. All rights reserved.
 * Author: Jarred Ward <jarred@simple.com>
 */

package com.simple.jdub

import scala.collection.mutable.Stack

trait TransactionProvider {
  def transactionExists: Boolean
  def currentTransaction: Transaction
  def begin(transaction: Transaction)
  def end: Unit
  def rollback: Unit
}

class TransactionManager extends TransactionProvider {
  case class TransactionState(transactions: Stack[Transaction])

  private val localTransactionStorage = new ThreadLocal[Option[TransactionState]] {
    override def initialValue = None
  }

  protected def ambientTransactionState = {
    localTransactionStorage.get
  }

  protected def ambientTransaction = {
    ambientTransactionState.flatMap(t => Some(t.transactions.head))
  }

  protected def currentTransactionState = {
    ambientTransactionState
      .getOrElse(throw new Exception("No transaction in current context"))
  }

  def currentTransaction = {
    ambientTransaction
      .getOrElse(throw new Exception("No transaction in current context"))
  }

  def transactionExists = {
    ambientTransactionState.isEmpty == false
  }

  def begin(transaction: Transaction) {
    if (!transactionExists) {
      localTransactionStorage.set(Some(new TransactionState(Stack(transaction))))
    } else {
      currentTransactionState.transactions.push(transaction)
    }
  }

  def end {
    if (!transactionExists) {
      throw new Exception("No transaction in current context")
    } else {
      currentTransactionState.transactions.pop
      if (currentTransactionState.transactions.isEmpty) {
        localTransactionStorage.set(None)
      }
    }
  }

  def rollback = {
    currentTransaction.rollback
  }
}
