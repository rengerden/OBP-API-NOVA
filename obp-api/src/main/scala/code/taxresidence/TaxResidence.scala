package code.taxresidence

import code.api.util.APIUtil
import code.remotedata.RemotedataTaxResidence
import com.openbankproject.commons.model.TaxResidence
import net.liftweb.common.Box
import net.liftweb.util.SimpleInjector

import scala.concurrent.Future

object TaxResidenceX extends SimpleInjector {

  val taxResidence = new Inject(buildOne _) {}

  def buildOne: TaxResidenceProvider =
    APIUtil.getPropsAsBoolValue("use_akka", false) match {
      case false  => MappedTaxResidenceProvider
      case true => RemotedataTaxResidence     // We will use Akka as a middleware
    }
}

trait TaxResidenceProvider {
  def getTaxResidence(customerId: String): Future[Box[List[TaxResidence]]]
  def createTaxResidence(customerId: String, domain: String, taxNumber: String): Future[Box[TaxResidence]]
  def deleteTaxResidence(taxResidenceId: String): Future[Box[Boolean]]
}


class RemotedataTaxResidenceCaseClasses {
  case class getTaxResidence(customerId: String)
  case class createTaxResidence(customerId: String, domain: String, taxNumber: String)
  case class deleteTaxResidence(taxResidenceId: String)
}

object RemotedataTaxResidenceCaseClasses extends RemotedataTaxResidenceCaseClasses