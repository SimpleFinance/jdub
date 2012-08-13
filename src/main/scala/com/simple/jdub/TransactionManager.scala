/**
 * © 2012 Simple Finance Technology Corp. All rights reserved.
 * Author: Jarred Ward <jarred@simple.com>
 */

package com.simple.jdub

class TransactionManager {
  private val localTransactionStorage = new ThreadLocal[Option[Transaction]] { 
    override def initialValue = None 
  }

  protected def ambientTransaction = {
    localTransactionStorage.get
  }

  def transactionExists = {
    ambientTransaction.isEmpty == false
  }

  def currentTransaction = {
    ambientTransaction
      .getOrElse(throw new Exception("No transaction in current context"))
  }

  def begin(transaction: Transaction) {
    if (transactionExists) {
      throw new Exception("Nesting transactions is not allowed")
    }

    localTransactionStorage.set(Some(transaction))
  }

  def end {
    localTransactionStorage.set(None)
  }

  def rollback = {
    currentTransaction.rollback
  }
}
