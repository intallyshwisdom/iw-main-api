package com.intallysh.widom.controller;

import java.io.InputStream;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.security.sasl.AuthenticationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intallysh.widom.dto.FileReqDto;
import com.intallysh.widom.dto.UpdateUserReqDto;
import com.intallysh.widom.exception.ForbiddenException;
import com.intallysh.widom.exception.ResourceNotProcessedException;
import com.intallysh.widom.service.FilesDetailService;
import com.intallysh.widom.service.UserActivityService;
import com.intallysh.widom.service.UserService;

import jakarta.validation.Valid;

@RestController
@PreAuthorize("hasAuthority('ADMIN_ROLE')")
@RequestMapping("/api/v1/admin")
public class AdminController {

	@Autowired
	private UserActivityService activityService;

	@Autowired
	private FilesDetailService filesDetailService;

	@Autowired
	private UserService userService;

//	FIle Services Implementations Started

	@PostMapping("/upload-file")
	public ResponseEntity<Map<String, Object>> uploadFile(@Valid @ModelAttribute FileReqDto fileReqDto)
			throws Exception {
		return ResponseEntity.ok().body(filesDetailService.uploadFile(fileReqDto));
	}

	@PostMapping("/upload-file/{userId}")
	public ResponseEntity<Map<String, Object>> uploadFileToUser(@PathVariable("userId") long userId,
			@Valid @ModelAttribute FileReqDto fileReqDto) throws Exception {
		return ResponseEntity.ok().body(filesDetailService.uploadFileByAdmin(fileReqDto, userId));
	}

	@GetMapping("/get-uploaded-file-years/{userId}")
	public ResponseEntity<Map<String, Object>> getUploadedFileYears(@PathVariable long userId) throws Exception {
		return ResponseEntity.ok().body(filesDetailService.getUploadedFileYears(userId));
	}

	@GetMapping("/get-filetransdetail-by-year-and-userid")
	public ResponseEntity<Map<String, Object>> getFileDetailByUserIdAndYear(@RequestParam long userId,
			@RequestParam int year, @RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize,
			@RequestParam(defaultValue = "reportDate") String sortBy,
			@RequestParam(defaultValue = "ASC") String sortingOrder) throws Exception {
		Sort by = Sort.by(sortBy);
		if (sortingOrder.equals("ASC")) {
			by = Sort.by(sortBy).ascending();
		} else {
			by = Sort.by(sortBy).descending();
		}
		Pageable paging = PageRequest.of(pageNo, pageSize, by);
		return ResponseEntity.ok().body(filesDetailService.getFileByYearAndUserId(userId, year, paging));
	}

	@GetMapping("/get-filetransdetail-by-date")
	public ResponseEntity<Map<String, Object>> getFileDetailByUserIdAndDate(@RequestParam long userId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") String fromDate,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") String toDate,
			@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "10") Integer pageSize,
			@RequestParam(defaultValue = "reportDate") String sortBy,
			@RequestParam(defaultValue = "ASC") String sortingOrder) {
		try {
			// Convert String dates to java.sql.Date objects
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date fromDateSql = new Date(sdf.parse(fromDate).getTime());
			Date toDateSql = new Date(sdf.parse(toDate).getTime());

			// Create Pageable object
			Sort sort = sortingOrder.equals("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
			Pageable paging = PageRequest.of(pageNo, pageSize, sort);

			// Call service method with converted dates
			return ResponseEntity.ok()
					.body(filesDetailService.getFileByYearAndDateType(userId, fromDateSql, toDateSql, paging));
		} catch (ParseException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd format for dates.");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceNotProcessedException("Error retrieving file details. Please try again later.");
		}
	}

	@GetMapping("/get-all-filetransdetail")
	public ResponseEntity<Map<String, Object>> getAllFileDetail(@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize,
			@RequestParam(defaultValue = "reportDate") String sortBy,
			@RequestParam(defaultValue = "ASC") String sortingOrder) throws Exception {
		Sort by = Sort.by(sortBy);
		if (sortingOrder.equals("ASC")) {
			by = Sort.by(sortBy).ascending();
		} else {
			by = Sort.by(sortBy).descending();
		}
		Pageable paging = PageRequest.of(pageNo, pageSize, by);
		return ResponseEntity.ok().body(filesDetailService.getAllFile(paging));
	}

	@GetMapping("/get-filedetail-by-transid/{transId}")
	public ResponseEntity<Map<String, Object>> getFileDetailByTransId(@PathVariable String transId) throws Exception {
//		Pageable paging = PageRequest.of(pageNo, pageSize,Sort.by(sortBy).descending());
		return ResponseEntity.ok().body(filesDetailService.getFileDetailByTransId(transId));
	}

	@GetMapping(path = "/get-file/{fileId}")
	public ResponseEntity<InputStreamResource> getFile(@PathVariable long fileId) throws Exception {
		Map<String, Object> file = filesDetailService.getFile(fileId);
		InputStream fileData = (InputStream) file.get("fileData");
		String fileName = (String) file.get("fileName");
		System.out.println("----- File Name :  --------" + fileName);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\""); // Wrap filename in
																									// quotes

		// Set Content-Type based on file extension or any other condition
		String contentType = determineContentType(fileName);
		headers.add(HttpHeaders.CONTENT_TYPE, contentType);

		return ResponseEntity.ok().headers(headers).body(new InputStreamResource(fileData));
	}

	private String determineContentType(String fileName) {
		// You can implement logic to determine Content-Type based on the file extension
		// For example, check if the file name ends with ".docx", ".pdf", etc.
		if (fileName.endsWith(".docx")) {
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		} else if (fileName.endsWith(".pdf")) {
			return "application/pdf";
		} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (fileName.endsWith(".png")) {
			return "image/png";
		}
		// Add more conditions as needed

		// Default to a generic content type if not determined
		return "application/octet-stream";
	}

//	File Services Implementations Ended

// User Profile Related Service Started

	@PutMapping("/user/update-profile")
	public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody @Valid UpdateUserReqDto reqDto) {
		Map<String, Object> map = new HashMap<>();
		try {
			map = userService.updateProfile(reqDto);
		} catch (AuthenticationException e) {
			e.printStackTrace();
			throw new ForbiddenException("Session Expired ! login and try again");
		}
		return ResponseEntity.ok().body(map);
	}

	@DeleteMapping("/user/delete/{userId}")
	public ResponseEntity<Map<String, Object>> deleteProfile(@PathVariable long userId) {
		Map<String, Object> map = new HashMap<>();
		try {
			map = userService.deleteProfile(userId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceNotProcessedException("User not deleted ...");
		}
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/users")
	public ResponseEntity<Map<String, Object>> getAllUsers(@RequestParam(defaultValue = "NOT_DELETED") String type,
			@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "10") Integer pageSize,
			@RequestParam(defaultValue = "userId") String sortBy,
			@RequestParam(defaultValue = "ASC") String sortingOrder) {
		Map<String, Object> map = new HashMap<>();
		Sort by = Sort.by(sortBy);
		if (sortingOrder.equals("ASC")) {
			by = Sort.by(sortBy).ascending();
		} else {
			by = Sort.by(sortBy).descending();
		}
		Pageable paging = PageRequest.of(pageNo, pageSize, by);
		try {
			map = userService.getAllUsers(type, paging);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceNotProcessedException("Users not Fetched ...");
		}
		return ResponseEntity.ok().body(map);
	}

	@PostMapping("/block-user/{userId}")
	public ResponseEntity<Map<String, Object>> blockUser(@PathVariable long userId) {
		Map<String, Object> map = new HashMap<>();
		try {
			map = userService.blockUser(userId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceNotProcessedException("User did not blocked ...");
		}
		return ResponseEntity.ok().body(map);
	}

	@PostMapping("/unblock-user/{userId}")
	public ResponseEntity<Map<String, Object>> unBlockUser(@PathVariable long userId) {
		Map<String, Object> map = new HashMap<>();
		try {
			map = userService.unBlockUser(userId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceNotProcessedException("User did not unblocked ...");
		}
		return ResponseEntity.ok().body(map);
	}

//	User Profile Related Service Ended

	@GetMapping("/get-user-activity/{userId}")
	public ResponseEntity<Map<String, Object>> getActivityChanges(@PathVariable long userId,
			@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "5") Integer pageSize,
			@RequestParam(defaultValue = "reportDate") String sortBy,
			@RequestParam(defaultValue = "reportDate") String sortingOrder) throws Exception {

		Sort by = Sort.by(sortBy);
		if (sortingOrder.equals("ASC")) {
			by = Sort.by(sortBy).ascending();
		} else {
			by = Sort.by(sortBy).descending();
		}
		Pageable paging = PageRequest.of(pageNo, pageSize, by);
		return ResponseEntity.ok().body(activityService.getUserActivity(userId, paging));
	}

	  @DeleteMapping("/delete-file/{transId}")
	    public ResponseEntity<Map<String, Object>> deleteFileByTransId(@PathVariable String transId) {
	        try {
	           
	            return ResponseEntity.ok().body(filesDetailService.deleteFileByTransId(transId));
	        } catch (Exception e) {
	            e.printStackTrace();
	            throw new ResourceNotProcessedException("Error Deleting file with transId " + transId);
	        }
	    }
	  @DeleteMapping("/delete-file-by-fileid/{fileId}")
	    public ResponseEntity<Map<String, Object>> deleteFileByFileId(@PathVariable String fileId) {
	        try {
	           
	            return ResponseEntity.ok().body(filesDetailService.deleteFileByFileId(fileId));
	        } catch (Exception e) {
	            e.printStackTrace();
	            throw new ResourceNotProcessedException("Error Deleting file with transId " + fileId);
	        }
	    }

}
