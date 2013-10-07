/**
 * © 2012 Simple Finance Technology Corp. All rights reserved.
 * Author: Jarred Ward <jarred@simple.com>
 */

package com.simple.jdub

trait TransactionProvider {
  def transactionExists: Boolean
  def currentTransaction: Transaction
  def begin(transaction: Transaction)
  def end: Unit
  def rollback: Unit
}

class TransactionManager extends TransactionProvider {
  case class TransactionState(transaction: Transaction, nestCount: Int)

  private val localTransactionStorage = new ThreadLocal[Option[TransactionState]] { 
    override def initialValue = None 
  }

  protected def ambientTransactionState = {
    localTransactionStorage.get
  }

  protected def ambientTransaction = {
    ambientTransactionState.flatMap(t => Some(t.transaction))
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
      localTransactionStorage.set(Some(new TransactionState(transaction, 0)))
    } else {
      val state = currentTransactionState.copy(nestCount = currentTransactionState.nestCount + 1)
      localTransactionStorage.set(Some(state))
    }
  }

  def end {
    if (!transactionExists) {
      throw new Exception("No transaction in current context")
    } else {
      if (currentTransactionState.nestCount == 0) {
        localTransactionStorage.set(None)
      } else {
        val state = currentTransactionState.copy(nestCount = currentTransactionState.nestCount - 1)
        localTransactionStorage.set(Some(state))
      }
    }
  }

  def rollback = {
    currentTransaction.rollback
  }
}
