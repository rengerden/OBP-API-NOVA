package code.bankconnectors.akka.actor

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import code.api.ResourceDocs1_4_0.MessageDocsSwaggerDefinitions._
import code.api.util.ErrorMessages.attemptedToOpenAnEmptyBox
import code.api.util.{APIUtil, OBPFromDate, OBPLimit, OBPToDate}
import code.bankconnectors.LocalMappedConnector._
import code.model.dataAccess.MappedBank
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model.{CreditLimit, _}
import net.liftweb.common.Box

import scala.collection.immutable.List


/**
  * This Actor acts in next way:
  */
class SouthSideActorOfAkkaConnector extends Actor with ActorLogging with MdcLoggable {

  def receive: Receive = waitingForRequest
  def successInBoundStatus: Status = Status("", Nil)

  private def waitingForRequest: Receive = {
    case OutBoundGetAdapterInfo(cc) =>
      val result = 
        InBoundGetAdapterInfo(
          InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext),
          successInBoundStatus,
          inboundAdapterInfoInternal
        )
      sender ! result   
    
    case OutBoundGetBanks(cc) =>
      val result: Box[List[MappedBank]] = getBanksLegacy(None).map(r => r._1)
      sender ! InBoundGetBanks(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext),successInBoundStatus, result.map(l => l.map(Transformer.bank(_))).openOrThrowException(attemptedToOpenAnEmptyBox))
    
    case OutBoundGetBank(cc, bankId) =>
      val result: Box[MappedBank] = getBankLegacy(bankId, None).map(r => r._1)
      sender ! InBoundGetBank(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext), successInBoundStatus, result.map(Transformer.bank(_)).openOrThrowException(attemptedToOpenAnEmptyBox) )
      
    case OutBoundCheckBankAccountExists(cc, bankId, accountId) =>
      val result: Box[BankAccount] = checkBankAccountExistsLegacy(bankId, accountId, None).map(r => r._1)
      sender ! InBoundCheckBankAccountExists(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext), successInBoundStatus, result.map(Transformer.bankAccount(_)).openOrThrowException(attemptedToOpenAnEmptyBox))
      
    case OutBoundGetBankAccount(cc, bankId, accountId) =>
      val result: Box[BankAccount] = getBankAccountLegacy(bankId, accountId, None).map(r => r._1)
      org.scalameta.logger.elem(result)
      sender ! InBoundGetBankAccount(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext), successInBoundStatus, result.map(Transformer.bankAccount(_)).openOrThrowException(attemptedToOpenAnEmptyBox))
      
    case OutBoundGetCoreBankAccounts(cc, bankIdAccountIds) =>
      val result: Box[List[CoreAccount]] = getCoreBankAccountsLegacy(bankIdAccountIds, None).map(r => r._1)
      sender ! InBoundGetCoreBankAccounts(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext), successInBoundStatus, result.map(l => l.map(Transformer.coreAccount(_))).openOrThrowException(attemptedToOpenAnEmptyBox))
      
    case OutBoundGetCustomersByUserId(cc, userId) =>
      val result: Box[List[Customer]] = getCustomersByUserIdLegacy(userId, None).map(r => r._1)
      sender ! InBoundGetCustomersByUserId(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext), successInBoundStatus, result.map(l => l.map(Transformer.toInternalCustomer(_))).openOrThrowException(attemptedToOpenAnEmptyBox))

    case OutBoundGetTransactions(cc, bankId, accountId, limit, offset, fromDate, toDate) =>
      val from = APIUtil.DateWithMsFormat.parse(fromDate)
      val to = APIUtil.DateWithMsFormat.parse(toDate)
      val result = getTransactionsLegacy(bankId, accountId, None, List(OBPLimit(limit), OBPFromDate(from), OBPToDate(to))).map(r => r._1)
      sender ! InBoundGetTransactions(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext), successInBoundStatus, result.getOrElse(Nil).map(Transformer.toInternalTransaction(_)))

    case OutBoundGetTransaction(cc, bankId, accountId, transactionId) =>
      val result = getTransactionLegacy(bankId, accountId, transactionId,  None).map(r => r._1)
      sender ! InBoundGetTransaction(InboundAdapterCallContext(cc.correlationId,cc.sessionId,cc.generalContext), successInBoundStatus, result.map(Transformer.toInternalTransaction(_)).openOrThrowException(attemptedToOpenAnEmptyBox))

    case message => 
      logger.warn("[AKKA ACTOR ERROR - REQUEST NOT RECOGNIZED] " + message)
      
  }

}


object Transformer {
  def bank(mb: MappedBank): BankCommons = 
    BankCommons(
      bankId=mb.bankId,
      shortName=mb.shortName,
      fullName=mb.fullName,
      logoUrl=mb.logoUrl,
      websiteUrl=mb.websiteUrl,
      bankRoutingScheme=mb.bankRoutingScheme,
      bankRoutingAddress=mb.bankRoutingAddress,
      swiftBic ="", //Not useful 
      nationalIdentifier ="" //Not useful 
    )
  
  
  def bankAccount(acc: BankAccount) =
    BankAccountCommons(
      accountId = acc.accountId,
      accountType = acc.accountType,
      balance = acc.balance,
      currency = acc.currency,
      name = acc.name,
      label = acc.label,
      iban = None,
      number = acc.number,
      bankId = acc.bankId,
      lastUpdate = acc.lastUpdate,
      branchId = acc.branchId,
      accountRoutingScheme = acc.accountRoutingScheme,
      accountRoutingAddress = acc.accountRoutingAddress,
      accountRoutings = Nil,
      accountRules = Nil,
      accountHolder = acc.accountHolder
    )
  
  
  def coreAccount(a: CoreAccount) =
    CoreAccount(
      id = a.id,
      label = a.label,
      bankId = a.bankId,
      accountType = a.accountType,
      accountRoutings = a.accountRoutings
    )

  def toInternalCustomer(customer: Customer): CustomerCommons = {
    CustomerCommons(
      customerId = customer.customerId,
      bankId = customer.bankId,
      number = customer.number,
      legalName = customer.legalName,
      mobileNumber = customer.mobileNumber,
      email = customer.email,
      faceImage = CustomerFaceImage(customer.faceImage.date,customer.faceImage.url),
      dateOfBirth = customer.dateOfBirth,
      relationshipStatus = customer.relationshipStatus,
      dependents = customer.dependents,
      dobOfDependents = customer.dobOfDependents,
      highestEducationAttained = customer.highestEducationAttained,
      employmentStatus = customer.employmentStatus,
      creditRating = CreditRating(customer.creditRating.rating, customer.creditRating.source),
      creditLimit = CreditLimit(customer.creditLimit.amount,customer.creditLimit.currency),
      kycStatus = customer.kycStatus,
      lastOkDate = customer.lastOkDate,
      title = customer.title,
      branchId = customer.branchId,
      nameSuffix = customer.nameSuffix
    )
  }
  

  def toInternalTransaction(t: Transaction): TransactionCommons = {
    TransactionCommons(
      uuid = t.uuid ,
      id  = t.id ,
      thisAccount = BankAccountCommons(
        accountId = t.thisAccount.accountId,
        accountType = t.thisAccount.accountType,
        balance = t.thisAccount.balance,
        currency = t.thisAccount.currency,
        name = t.thisAccount.name,
        label = t.thisAccount.label,
        iban = t.thisAccount.iban,
        number = t.thisAccount.number,
        bankId = t.thisAccount.bankId,
        lastUpdate = t.thisAccount.lastUpdate,
        branchId = t.thisAccount.branchId,
        accountRoutingScheme = t.thisAccount.accountRoutingScheme,
        accountRoutingAddress = t.thisAccount.accountRoutingAddress,
        accountRoutings = t.thisAccount.accountRoutings,
        accountRules = t.thisAccount.accountRules,
        accountHolder = t.thisAccount.accountHolder
      ) ,
      otherAccount = t.otherAccount ,
      transactionType = t.transactionType ,
      amount = t.amount ,
      currency = t.currency ,
      description = t.description ,
      startDate = t.startDate ,
      finishDate = t.finishDate ,
      balance = t.balance
    )
  }
}

