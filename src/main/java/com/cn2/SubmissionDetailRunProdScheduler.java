package com.cn2;
/**
 * @author hyang
 *
 */
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cn2.model.Constant;
import com.cn2.model.RESTFulServiceConsumer;

@Component
public class SubmissionDetailRunProdScheduler {
	@Autowired
	private RESTFulServiceConsumer restConsumer;

	public void run() {

		try {

			String dateParam = new Date().toString();
									
			System.out.println("SubmissionDetailRunProdScheduler Start"+dateParam);
			String token = RESTFulServiceConsumer.getCn2loginToken(Constant.prod_baseHost, Constant.prod_userId,
					Constant.prod_password);
			String voToken = 	RESTFulServiceConsumer.getAuthSessionProviderToken(Constant.prod_voHost, Constant.prod_voUserId,
			        "api4VOsfdata", Constant.prod_voPassword,"281930572565008912");
			
			RESTFulServiceConsumer.processCn2DataForSubmissionDetail(Constant.prod_apiHost,Constant.prod_voHost, token,voToken,Constant.prod_keyId, "61f0fbc4-ca96-47c6-8c17-9cf4668afe6f", "MobileUser",
					"createdDate", 0, 20,1000);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
