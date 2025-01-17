package hrms.bussiness.concretes;

import java.time.LocalDate;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import hrms.bussiness.abstracts.AuthService;
import hrms.bussiness.abstracts.EmployerService;
import hrms.bussiness.abstracts.JobCandidateService;
import hrms.bussiness.abstracts.UserService;
import hrms.bussiness.abstracts.VerificationCodeService;
import hrms.core.entities.User;
import hrms.core.results.DataResult;
import hrms.core.results.ErrorDataResult;
import hrms.core.results.ErrorResult;
import hrms.core.results.Result;
import hrms.core.results.SuccessDataResult;
import hrms.core.results.SuccessResult;
import hrms.core.utilities.verifications.VerificationService;
import hrms.entities.concretes.Employer;
import hrms.entities.concretes.JobCandidate;
import hrms.entities.concretes.VerificationCode;
import hrms.entities.dtos.EmployerRegisterDto;
import hrms.entities.dtos.JobCandidateRegisterDto;
import hrms.entities.dtos.UserForLoginDto;
import lombok.var;

@Service
public class AuthManager implements AuthService {


	private EmployerService employerService;
	private JobCandidateService jobCandidateService;
	private UserService userService;
	private VerificationService verificationService;
	private VerificationCodeService verificationCodeService;

	private ModelMapper modelMapper;

	@Autowired
	public AuthManager(ModelMapper modelMapper, VerificationCodeService verificationCodeService,
			VerificationService verificationService, EmployerService employerService,
			JobCandidateService jobCandidateService, UserService userService) {
		super();

		this.employerService = employerService;
		this.jobCandidateService = jobCandidateService;
		this.userService = userService;
		this.modelMapper = modelMapper;
		this.verificationService = verificationService;
		this.verificationCodeService = verificationCodeService;

	}

	@Override
	public Result employerRegister(EmployerRegisterDto employerRegisterDto) {

		if(!checkIfEqualPasswordAndConfirmPassword(employerRegisterDto.getPassword(),employerRegisterDto.getConfirmPassword())) {
			
			return new ErrorResult("Passwords do not match !");
		}
		
		Employer employer = modelMapper.map(employerRegisterDto, Employer.class);

		var result = this.employerService.add(employer);

		
		if (result.isSuccess()) {
			this.generateVerificationCode(employer.getId());

			return new SuccessResult("Kayıt gerçekleşti");
		}
		return new ErrorResult("something's gone wrong... Please try again.");

	}

	@Override
	public Result jobCandidateRegister(JobCandidateRegisterDto jobCandidateRegisterDto) {

		if (!checkIfEqualPasswordAndConfirmPassword(jobCandidateRegisterDto.getPassword(),
				jobCandidateRegisterDto.getConfirmedPassword())) {

			return new ErrorResult("Passwords do not match !");
		}

		JobCandidate jobcandidate = modelMapper.map(jobCandidateRegisterDto, JobCandidate.class);

		var result = this.jobCandidateService.add(jobcandidate);

		if (result.isSuccess()) {

			this.generateVerificationCode(jobcandidate.getId());
			return new SuccessResult("Kayıt gerçekleşti");

		}
		return new ErrorResult(result.getMessage());

	}

	@Override
	public DataResult<User> login(UserForLoginDto userForLoginDto) {
		var userToCheck = userService.findByEmail(userForLoginDto.getEmail());

		
		if (userToCheck.getData() == null) {
			return new ErrorDataResult<>("Giriş yapılamadı");
		}

		return new SuccessDataResult<User>(userToCheck.getData(), "Giriş yapıldı");
	}

	@Override
	public Result verifyEmail(int user_id, String activationCode) {

		var result = this.verificationCodeService.getByUserIdAndVerificationCode(user_id, activationCode);

		if (!this.isVerificationCodeExist(user_id, activationCode).isSuccess()
				|| !this.isVerificationCodeActive(user_id, activationCode).isSuccess()
				|| !isExpired(user_id, activationCode).isSuccess()) {

			return new ErrorResult();
		}

		if (!this.setCandidateActivationCode(user_id).isSuccess()
				&& !this.setEmployerActivationCode(user_id).isSuccess()) {
			return new ErrorResult("User couldn't find");
		}

		VerificationCode verificationCode = result.getData();

		verificationCode.setConfirmedDate(LocalDate.now());
		verificationCode.setIsActive(true);
		this.verificationCodeService.update(verificationCode);

		return new SuccessResult("Verified !");

	}

	@Override
	public Result userExists(String email) {
		return null;
	}

	private Result generateVerificationCode(int userId) {
		String code = this.verificationService.codeGenerator();
		this.verificationService.sendVerificationCode(code);
		VerificationCode verificationCode = new VerificationCode(userId, code, LocalDate.now().plusDays(1));
		this.verificationCodeService.add(verificationCode);
		return new SuccessResult("User Registered !");
	}

	private Result isVerificationCodeExist(int user_id, String activationCode) {

		if (this.verificationCodeService.getByUserIdAndVerificationCode(user_id, activationCode).getData() == null) {
			return new ErrorResult("Verification Code is wrong !");
		}
		return new SuccessResult();

	}

	private Result isExpired(int user_id, String activationCode) {

		if (this.verificationCodeService.getByUserIdAndVerificationCode(user_id, activationCode).getData()
				.getExpiredDate().isBefore(LocalDate.now())) {

			return new ErrorResult("Verification Code is Expired");
		}
		return new SuccessResult();
	}

	private Result isVerificationCodeActive(int user_id, String activationCode) {

		if (this.verificationCodeService.getByUserIdAndVerificationCode(user_id, activationCode).getData()
				.getIsActive()) {

			return new ErrorResult("Verification Code is already active");
		}
		return new SuccessResult();
	}

	private Result setCandidateActivationCode(int user_id) {
		if (this.jobCandidateService.findById(user_id).getData() != null) {

			JobCandidate jobcandidate = this.jobCandidateService.findById(user_id).getData();

			jobcandidate.setIsEmailVerified(true);
			jobcandidate.setIsActive(true);

			this.jobCandidateService.update(jobcandidate);

			return new SuccessResult();
		}

		return new ErrorResult();
	}

	private Result setEmployerActivationCode(int user_id) {

		if (this.employerService.findById(user_id).getData() != null) {

			Employer employer = this.employerService.findById(user_id).getData();

			employer.setIsEmailVerified(true);
			employer.setIsActive(true);

			this.employerService.update(employer);

			return new SuccessResult();
		}

		return new ErrorResult();
	}

	private boolean checkIfEqualPasswordAndConfirmPassword(String password, String confirmPassword) {

		if (!password.equals(confirmPassword)) {
			return false;
		}

		return true;
	}

}
