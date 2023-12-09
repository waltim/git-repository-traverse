package com.unb.traverse.traverse;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.unb.traverse.traverse.utils.Interval;

import lombok.val;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class App {

	static String repositoryPath = "D:\\my-workspace\\dataset\\meteor";

	private static List<String> removerDuplicatas(List<String> lista) {
		Set<String> conjuntoSemRepeticoes = new LinkedHashSet<>(lista);
		return new ArrayList<>(conjuntoSemRepeticoes);
	}

	public static void main(String[] args) {
		try (Git git = Git.open(new java.io.File(repositoryPath)); Repository repository = git.getRepository()) {
			String latestBranchName = getBranchNameFromLatestCommit(repository, git);
			List<String> targetBranches = removerDuplicatas(
					Arrays.asList("main", "master", latestBranchName.substring(latestBranchName.lastIndexOf("/") + 1)));
			System.out.println(targetBranches);

			for (Ref branchRef : git.branchList().setListMode(ListMode.REMOTE).call()) {
				String branchName = repository.shortenRefName(branchRef.getName());

				boolean startsWithAny = false;
				for (String branch : targetBranches) {
//					System.out.println(branchName);
					if (branchName.substring(branchName.lastIndexOf("/") + 1).equals(branch)) {
						startsWithAny = true;
						break;
					}
				}

				if (startsWithAny) {
					System.out.println("Branch: " + branchName);
					ObjectId branchObjectId = repository.resolve(branchName);
					List<RevCommit> commits = listCommits(repository, branchObjectId);
					System.out.println("Número de Commits: " + commits.size());
					System.out.println("------------------------");

					traverse(commits, repository, git, branchName);
					break;
				} else {
					continue;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static List<RevCommit> listCommits(Repository repository, ObjectId branchObjectId) throws Exception {
		try (RevWalk revWalk = new RevWalk(repository)) {
			revWalk.markStart(revWalk.parseCommit(branchObjectId));
			return StreamSupport.stream(revWalk.spliterator(), false)
					.filter(r -> r.getParentCount() == 1)
					.collect(Collectors.toList());
		}
	}

	private static String getBranchNameFromLatestCommit(Repository repository, Git git) throws Exception {
		try (RevWalk revWalk = new RevWalk(repository)) {
			var mainBranch = "";
			val branches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().stream()
					.filter(n -> n.getName().equals("refs/remotes/origin/HEAD")).findFirst();

			if (branches.isPresent()) {
				mainBranch = branches.get().getTarget().getName().substring("refs/remotes/origin/".length());
				return mainBranch;
			} else {
				throw new Exception();
			}
		}
	}

	private static void traverse(List<RevCommit> revisions, Repository repository, Git git, String branch)
			throws Exception {

		Date initialDate = new SimpleDateFormat("yyyy-MM-dd").parse("2015-07-06");
		Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse("2015-10-10");
		int steps = 7;

		val interval = Interval.builder().begin(initialDate).end(endDate).build();

		val commits = new HashMap<Date, ObjectId>();
		val commitDates = new HashSet<Date>(); // Use a HashSet for unique dates

		Date previous = null;

		for (RevCommit revision : revisions) {
			val commitTimeInSeconds = revision.getCommitTime();
			val current = new Date((long) commitTimeInSeconds * 1000);
			if (current.compareTo(interval.begin) >= 0 && current.compareTo(interval.end) <= 0) {
				// only add commits that fit the interval

				if (previous == null || Interval.diff(current, previous, Interval.Unit.Days) >= steps) {
					commitDates.add(current);
					previous = current;

					// add just the date are not added
					if (!commits.containsKey(current)) {
						commits.put(current, revision.toObjectId());
					}
				}
			}
		}

		List<Date> sortedCommitDates = new ArrayList<>(commitDates);
		Collections.sort(sortedCommitDates);

		val totalGroups = sortedCommitDates.size();
		val totalCommits = commits.size();

		for (Date current : sortedCommitDates) {
			ObjectId obj = commits.get(current);
			RevCommit commit = objectIdToRevCommit(repository, obj);
//			String branchName = getNonMergeBranchForCommitObjectId(repository, git, obj, branch);
			if (commit != null) {
				val commitTimeInSeconds = commit.getCommitTime();
				val commitCurrent = new Date((long) commitTimeInSeconds * 1000);
//				System.out.println("Date: " + commitCurrent + ", Commit:" + commit.getName() + ", Parents: "
//						+ commit.getParentCount() + ", BranchName: " + branchName);
				System.out.println("Date: " + commitCurrent + ", Commit:" + commit.getName() + ", Parents: "
						+ commit.getParentCount());
			}
		}
	}

	public static RevCommit objectIdToRevCommit(Repository repository, ObjectId objectId) throws Exception {
		try (RevWalk revWalk = new RevWalk(repository)) {
			return revWalk.parseCommit(objectId);
		}
	}

	private static String getNonMergeBranchForCommitObjectId(Repository repository, Git git, ObjectId commitObjectId,
			String origin) throws Exception {
		List<Ref> branches = git.branchList().setListMode(ListMode.REMOTE).call();
		try (RevWalk revWalk = new RevWalk(repository)) {
			RevCommit commit = revWalk.parseCommit(commitObjectId);
			String headBranchName = getBranchNameFromLatestCommit(repository, git);
			for (Ref branch : branches) {
				if (isCommitInBranch(repository, commit, branch.getObjectId())) {
					if (commit.getParentCount() == 1) {
						if (origin.equals(repository.shortenRefName(branch.getName()))
								&& origin.endsWith(headBranchName)) {
							return repository.shortenRefName(branch.getName());
						}
					}
				}
			}
		}
		return null;
	}

	private static boolean isCommitInBranch(Repository repository, RevCommit commit, ObjectId branchObjectId)
			throws Exception {
		try (RevWalk revWalk = new RevWalk(repository)) {
			for (RevCommit revCommit : listCommits(repository, branchObjectId)) {
				if (revCommit.getName().equals(commit.getName()) && isMergeCommit(revCommit) && isMergeCommit(commit)) {
					return true;
				}
			}
			return false;
		}
	}

	private static boolean isMergeCommit(RevCommit commit) {
		// Verifica se a mensagem do commit contém a palavra "merge"
		if (commit.getShortMessage().toLowerCase().contains("merge")
				|| commit.getFullMessage().toLowerCase().contains("merge")) {
			return false;
		}
		return true;
	}

}