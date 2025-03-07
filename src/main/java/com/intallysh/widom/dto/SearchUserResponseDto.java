package com.intallysh.widom.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserResponseDto {

	private long userId;
	private String email;
	private String phone;
	private String name;
	
	
}
