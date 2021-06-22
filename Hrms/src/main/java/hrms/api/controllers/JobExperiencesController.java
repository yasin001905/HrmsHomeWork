package hrms.api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hrms.bussiness.abstracts.JobCandidateService;
import hrms.bussiness.abstracts.JobExperienceService;
import hrms.core.results.DataResult;
import hrms.core.results.Result;

import hrms.entities.concretes.JobCandidate;
import hrms.entities.concretes.JobExperience;

@RestController
@RequestMapping("/api/auth")
public class JobExperiencesController {

	private JobExperienceService jobExperienceService;
	private JobCandidateService jobCandidateService;

	@Autowired
	public JobExperiencesController(JobExperienceService jobExperienceService,
			JobCandidateService jobCandidateService) {
		super();
		this.jobExperienceService = jobExperienceService;
		this.jobCandidateService = jobCandidateService;
	}

	@PostMapping("/add")
	public Result add(@RequestParam int id, @RequestBody JobExperience jobExperience) {
		JobCandidate jobCandidate = jobCandidateService.findById(id).getData();

		jobExperience.setJobCandidate(jobCandidate);

		return jobExperienceService.add(jobExperience);

	}

	@GetMapping("/findById")
	public DataResult<JobExperience> findById(@RequestParam int id) {

		return jobExperienceService.findById(id);

	}

	@GetMapping("/findAll")
	public DataResult<List<JobExperience>> findAll() {
		return jobExperienceService.findAll();
	}
}
