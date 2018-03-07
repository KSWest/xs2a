package de.adorsys.aspsp.xs2a.spi.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
@ApiModel(description = "Tpp Message Information", value = "TppMessageInformation")
@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class TppMessageInformation {

	@ApiModelProperty(value = "Category of the error permitted", required = true, example = "Error")
	private String category;
	
	@ApiModelProperty(value = "Code", required = true)
	private MessageCode code;

	@ApiModelProperty(value = "Path", example = "")
	private String path;

	@ApiModelProperty(value = "Additional explanation text", example = "Additional Text")
    @Size(max = 512)
	private String text;

}
